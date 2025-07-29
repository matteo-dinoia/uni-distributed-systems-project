package tester;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import messages.Message;
import messages.client.DataMsg;
import messages.client.ResponseMsgs;
import messages.client.StatusMsg;
import messages.control.ControlMsg;
import node.NodeActor;
import node.NodeState;
import utils.Config;
import utils.Ring;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class Tester implements AutoCloseable {
    private final ActorTestKit testKit;
    private final Ring<ActorRef<Message>> group = new Ring<>();
    private static final Duration TIMEOUT_PROBE = Duration.ofSeconds(5);


    /// GENERAL UTILITIES
    public Tester(TestKitJunitResource resource, Set<Integer> initialNodes) {
        this.testKit = resource.testKit();
        if (initialNodes.size() < Config.N)
            throw new RuntimeException("Cannot initialize because too little nodes");

        initializeMembers(initialNodes);
        System.out.println();
    }

    private void initializeMembers(Set<Integer> initialNodes) {
        for (Integer id : initialNodes)
            group.put(id, testKit.spawn(NodeActor.create(id), "Node" + id));

        TestProbe<Message> probe = getProbe();
        for (ActorRef<Message> node : group.getHashMap().values())
            send(probe, node, new StatusMsg.InitialMembers(group.getHashMap()));


        for (int i = 0; i < group.size(); i++) {
            Serializable content = probe.receiveMessage(TIMEOUT_PROBE).content();
            if (!(content instanceof ControlMsg.InitialMembersAck()))
                throw new RuntimeException("Wrong message received");
        }
    }

    private TestProbe<Message> getProbe() {
        return testKit.createTestProbe(Message.class);
    }

    private ActorRef<Message> getNode(Integer nodeId) {
        if (nodeId == null)
            throw new RuntimeException("Node key is null");
        ActorRef<Message> node = group.get(nodeId);
        if (node == null)
            throw new RuntimeException("Node doesn't exist");
        return node;
    }

    private void send(TestProbe<Message> sender, ActorRef<Message> recipient, Serializable content) {
        Message crashMsg = new Message(sender.getRef(), content);
        recipient.tell(crashMsg);
    }

    private void assertOrThrows(boolean condition, String error) {
        if (!condition)
            throw new RuntimeException(error);
    }


    /// OPERATION UTILITIES:

    public Client getClient() {
        return new Client(getProbe());
    }

    public NodeState getNodeState(int nodeId) {
        ActorRef<Message> node = getNode(nodeId);
        TestProbe<Message> probe = getProbe();
        send(probe, node, new ControlMsg.DebugGetCurrentState());

        Serializable content = probe.receiveMessage(TIMEOUT_PROBE).content();
        if (!(content instanceof ControlMsg.DebugCurrentState(NodeState realState)))
            throw new RuntimeException("Wrong message received");

        return realState;
    }

    /// GET+UPDATE: send ReadRequest and Update, wait for ReadResponse and WriteFullyCompleted
    /// Return number of successful operation
    public int clientOperation(Map<Client, ClientOperation> operations) {
        TestProbe<Message> probe = getProbe();

        for (var operation : operations.entrySet()) {
            Client client = operation.getKey();
            ClientOperation op = operation.getValue();

            switch (op) {
                case ClientOperation.Read(int key, int nodeId) -> {
                    Integer lastVersion = client.getKeyLatestVersion(key);
                    send(client.getReceiver(), getNode(nodeId), new DataMsg.Get(key, lastVersion));
                }
                case ClientOperation.Write(int key, int nodeId) -> {
                    String newValue = "randValue=" + new Random().nextInt();
                    send(client.getReceiver(), getNode(nodeId), new DataMsg.Update(key, newValue));
                }
                default -> throw new IllegalStateException("Someone made a new subclass and didn't add it here");
            }
        }


        int successful = 0;
        for (var operation : operations.entrySet()) {
            Client client = operation.getKey();
            boolean isRead = (operation.getValue() instanceof ClientOperation.Read ignored);

            int key = switch (operation.getValue()) {
                case ClientOperation.Read(int k, int ignored) -> k;
                case ClientOperation.Write(int k, int ignored) -> k;
                default -> throw new IllegalStateException("Someone made a new subclass and didn't add it here");
            };

            int nodeId = switch (operation.getValue()) {
                case ClientOperation.Read(int ignored, int node) -> node;
                case ClientOperation.Write(int ignored, int node) -> node;
                default -> throw new IllegalStateException("Someone made a new subclass and didn't add it here");
            };

            Serializable content = client.getReceiver().receiveMessage(TIMEOUT_PROBE).content();

            switch (content) {
                case ResponseMsgs.ReadSucceeded msg -> {
                    assertOrThrows(isRead, "Unexpected Message received");
                    client.setKeyLatestVersion(msg.key(), msg.version());
                    successful++;
                }
                case ResponseMsgs.ReadResultFailed ignored -> {
                    assertOrThrows(isRead, "Unexpected Message received");
                }
                case ResponseMsgs.ReadResultInexistentValue ignored -> {
                    assertOrThrows(isRead, "Unexpected Message received");
                    successful++;
                }
                case ResponseMsgs.ReadTimeout ignored -> {
                    assertOrThrows(isRead, "Unexpected Message received");
                }
                case ResponseMsgs.WriteSucceeded msg -> {
                    assertOrThrows(!isRead, "Unexpected Message received");
                    client.setKeyLatestVersion(msg.key(), msg.newVersion());
                    successful++;
                }
                case ResponseMsgs.WriteTimeout ignored -> {
                    assertOrThrows(!isRead, "Unexpected Message received");
                }
                default -> throw new RuntimeException("Unexpected Message received");
            }

            if (!isRead)
                client.getReceiver().expectMessage(TIMEOUT_PROBE, new Message(getNode(nodeId), new ControlMsg.WriteFullyCompleted()));
        }

        return successful;
    }

    /// JOIN: send StatusMsg.Join wrapped in Message and wait for JoinAck
    public boolean join(int nodeId) {
        ActorRef<Message> node = testKit.spawn(NodeActor.create(nodeId), "Node" + nodeId);

        TestProbe<Message> probe = getProbe();

        Integer leftKey = group.getCeilKey(nodeId - 1);
        ActorRef<Message> bootstrapNode = getNode(leftKey);

        send(probe, node, new StatusMsg.Join(bootstrapNode));
        Serializable content = probe.receiveMessage(TIMEOUT_PROBE).content();

        if (!(content instanceof ControlMsg.JoinAck(boolean joined)))
            throw new RuntimeException("Wrong message received");

        if (joined)
            group.put(nodeId, node);
        else
            testKit.stop(node);
        return joined;
    }

    /// LEAVE: send StatusMsg.Leave wrapped in Message and wait for LeaveAck
    public boolean leave(int nodeId) {
        ActorRef<Message> node = getNode(nodeId);
        TestProbe<Message> probe = getProbe();

        send(probe, node, new StatusMsg.Leave());
        Serializable content = probe.receiveMessage(TIMEOUT_PROBE).content();

        if (!(content instanceof ControlMsg.LeaveAck(boolean left)))
            throw new RuntimeException("Wrong message received");
        return left;
    }

    /// RECOVER: send StatusMsg.Recover wrapped in Message and wait for RecoverAck
    public boolean recover(int nodeId) {
        ActorRef<Message> node = getNode(nodeId);
        TestProbe<Message> probe = getProbe();

        Integer leftKey = group.getCeilKey(nodeId - 1);
        ActorRef<Message> bootstrapNode = getNode(leftKey);

        send(probe, node, new StatusMsg.Recover(bootstrapNode));
        Serializable content = probe.receiveMessage(TIMEOUT_PROBE).content();

        if (!(content instanceof ControlMsg.RecoverAck(boolean recovered)))
            throw new RuntimeException("Wrong message received");
        return recovered;
    }

    /// CRASH: send StatusMsg.Crash wrapped in Message and wait for CrashAck
    public void crash(int nodeId) {
        ActorRef<Message> node = getNode(nodeId);
        TestProbe<Message> probe = getProbe();
        send(probe, node, new StatusMsg.Crash());
        probe.expectMessage(TIMEOUT_PROBE, new Message(node, new ControlMsg.CrashAck()));
    }

    @Override
    public void close() {
        for (var node : group.getHashMap().values())
            testKit.stop(node);
    }
}