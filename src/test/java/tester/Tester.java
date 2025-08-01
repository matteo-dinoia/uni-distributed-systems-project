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
import node.SendableData;
import utils.Config;
import utils.Ring;
import utils.Utils;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;


public class Tester implements AutoCloseable {
    private final ActorTestKit testKit;
    private final Ring<ActorRef<Message>> group = new Ring<>();
    private static final Duration TIMEOUT_PROBE = Config.TIMOUT_PROBE;


    /// GENERAL UTILITIES
    public Tester(TestKitJunitResource resource, Set<Integer> initialNodes) {
        this.testKit = resource.testKit();
        if (initialNodes.size() < Config.N)
            throw new RuntimeException("Cannot initialize because too little nodes");

        initializeMembers(initialNodes);

        Utils.debugPrint("");
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

    public Client getClient() {
        return new Client(getProbe());
    }

    // GET DEBUG INFO

    public NodeState getNodeState(int nodeId) {
        ActorRef<Message> node = getNode(nodeId);
        TestProbe<Message> probe = getProbe();
        send(probe, node, new ControlMsg.DebugCurrentStateRequest());

        Serializable content = probe.receiveMessage(TIMEOUT_PROBE).content();
        if (!(content instanceof ControlMsg.DebugCurrentStateResponse(NodeState realState)))
            throw new RuntimeException("Wrong message received");

        Utils.debugPrint("");
        return realState;
    }

    /// NodeID -> Storage (key -> Data Element)
    public StorageTester getNodeStorages() {
        Map<Integer, Map<Integer, SendableData.Debug>> res = new HashMap<>();
        TestProbe<Message> probe = getProbe();

        for (ActorRef<Message> node : group.getHashMap().values())
            send(probe, node, new ControlMsg.DebugCurrentStorageRequest());

        for (var _ : group.getHashMap().keySet()) {
            Serializable content = probe.receiveMessage(TIMEOUT_PROBE).content();
            if (!(content instanceof ControlMsg.DebugCurrentStorageResponse(
                    int id, Map<Integer, SendableData.Debug> data
            )))
                throw new RuntimeException("Wrong message received");

            res.put(id, data);
        }

        return new StorageTester(res);
    }

    // OPERATION TESTER

    public boolean read(Client client, int key, int nodeId) {
        if (client == null)
            client = getClient();
        var succ = clientOperations(Map.ofEntries(entry(client, ClientOperation.newRead(key, nodeId))));
        return !succ.isEmpty();
    }

    public boolean write(Client client, int key, int nodeId) {
        if (client == null)
            client = getClient();
        var succ = clientOperations(Map.ofEntries(entry(client, ClientOperation.newWrite(key, nodeId))));
        return !succ.isEmpty();
    }

    /// GET+UPDATE: send ReadRequest and Update, wait for ReadResponse and WriteFullyCompleted
    /// Return number of successful operation
    public Map<Client, ClientOperation> clientOperations(Map<Client, ClientOperation> operations) {
        for (var operation : operations.entrySet()) {
            Client client = operation.getKey();
            ClientOperation op = operation.getValue();

            if (op.isRead()) {
                Integer lastVersion = client.getKeyLatestVersion(op.key());
                send(client.getReceiver(), getNode(op.nodeId()), new DataMsg.Get(op.key(), lastVersion));
            } else {
                send(client.getReceiver(), getNode(op.nodeId()), new DataMsg.Update(op.key(), op.newValue()));
            }
        }


        Map<Client, ClientOperation> successfulOp = new HashMap<>();
        for (var entry : operations.entrySet()) {
            Client client = entry.getKey();
            ClientOperation operation = entry.getValue();
            boolean isRead = operation.isRead();
            int nodeId = operation.nodeId();

            Serializable content = client.getReceiver().receiveMessage(TIMEOUT_PROBE).content();

            boolean succWrite = false;
            switch (content) {
                case ResponseMsgs.ReadSucceeded msg -> {
                    assert isRead : "Unexpected Message received";
                    client.setKeyLatestVersion(msg.key(), msg.version());
                    successfulOp.put(client, operation);
                }
                case ResponseMsgs.ReadResultFailed _ -> {
                    assert isRead : "Unexpected Message received";
                }
                case ResponseMsgs.ReadResultInexistentValue _ -> {
                    assert isRead : "Unexpected Message received";
                    successfulOp.put(client, operation);
                }
                case ResponseMsgs.ReadTimeout _ -> {
                    assert isRead : "Unexpected Message received";
                }
                case ResponseMsgs.WriteSucceeded msg -> {
                    assert !isRead : "Unexpected Message received";
                    client.setKeyLatestVersion(msg.key(), msg.newVersion());
                    successfulOp.put(client, operation);
                    succWrite = true;
                }
                case ResponseMsgs.WriteTimeout _ -> {
                    assert !isRead : "Unexpected Message received";
                }
                default -> throw new RuntimeException("Unexpected Message received");
            }

            if (succWrite)
                client.getReceiver().expectMessage(TIMEOUT_PROBE, new Message(getNode(nodeId), new ControlMsg.WriteFullyCompleted()));
        }

        Utils.debugPrint("");
        return successfulOp;
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
        Utils.debugPrint("");
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

        if (left) {
            group.remove(nodeId);
            testKit.stop(node);
        }
        Utils.debugPrint("");
        return left;
    }

    /// RECOVER: send StatusMsg.Recover wrapped in Message and wait for RecoverAck
    public boolean recover(int nodeId) {
        ActorRef<Message> node = getNode(nodeId);
        TestProbe<Message> probe = getProbe();

        Integer leftKey = group.getFloorKey(nodeId - 1);
        ActorRef<Message> bootstrapNode = getNode(leftKey);

        send(probe, node, new StatusMsg.Recover(bootstrapNode));
        Serializable content = probe.receiveMessage(TIMEOUT_PROBE).content();

        if (!(content instanceof ControlMsg.RecoverAck(boolean recovered)))
            throw new RuntimeException("Wrong message received");
        Utils.debugPrint("");
        return recovered;
    }

    /// CRASH: send StatusMsg.Crash wrapped in Message and wait for CrashAck
    public void crash(int nodeId) {
        ActorRef<Message> node = getNode(nodeId);
        TestProbe<Message> probe = getProbe();
        send(probe, node, new StatusMsg.Crash());
        probe.expectMessage(TIMEOUT_PROBE, new Message(node, new ControlMsg.CrashAck()));
        Utils.debugPrint("");
    }

    @Override
    public void close() {
        for (var node : group.getHashMap().values())
            testKit.stop(node);
    }
}