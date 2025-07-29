package tester;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import messages.Message;
import messages.client.StatusMsg;
import messages.control.ControlMsg;
import node.NodeActor;
import node.NodeState;
import utils.Config;
import utils.Pair;
import utils.Ring;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
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


    /// OPERATION UTILITIES:

    public NodeState getNodeState(int nodeId) {
        ActorRef<Message> node = getNode(nodeId);
        TestProbe<Message> probe = getProbe();
        send(probe, node, new ControlMsg.DebugGetCurrentState());

        Serializable content = probe.receiveMessage(TIMEOUT_PROBE).content();
        if (!(content instanceof ControlMsg.DebugCurrentState(NodeState realState)))
            throw new RuntimeException("Wrong message received");

        return realState;
    }

    /**
     * GET+UPDATE: send ReadRequest and Update, wait for ReadResponse and WriteFullyCompleted
     */
    public boolean operationIO(List<Integer> getNodeIds, List<Pair<Integer, String>> updateOps) {
//        TestProbe<Message> readProbe = testKit.createTestProbe(Message.class);
//        TestProbe<Message> writeProbe = testKit.createTestProbe(Message.class);
//
//        int reqId = 1;
//        for (Integer id : getNodeIds) {
//            group.get(id).tell(
//                    new Message(readProbe.getRef(), new NodeDataMsg.ReadRequest(reqId++, id)
//                    )
//            );
//        }
//        for (Pair<Integer, String> op : updateOps) {
//            int id = op.getLeft();
//            String v = op.getRight();
//            group.get(id).tell(new Message(writeProbe.getRef(), new NodeDataMsg.WriteRequest(reqId++, id, v, 0)));
//        }
//
//        try {
//            for (int i = 0; i < getNodeIds.size(); i++) {
//                //readProbe.expectMessage(Duration.ofSeconds(3));
//            }
//            for (int i = 0; i < updateOps.size(); i++) {
//                //writeProbe.expectMessage(Duration.ofSeconds(3));
//            }
//            return true;
//        } catch (AssertionError ex) {
//            return false;
//        }
        throw new UnsupportedOperationException("Unimplemented");
    }

    /// JOIN: send StatusMsg.Join wrapped in Message and wait for JoinAck
    public boolean join(int joinerId) {
//        ActorRef<Message> newNode = testKit.spawn(NodeActor.create(joinerId), "node" + joinerId);
//        group.put(joinerId, newNode);
//
//        TestProbe<Message> probe = testKit.createTestProbe(Message.class);
//
//        ActorRef<Message> bootstrapNode = null;
//
//        for (var entry : group.entrySet()) {
//            if (!entry.getKey().equals(joinerId)) {
//                bootstrapNode = entry.getValue();
//                break;
//            }
//        }
//
//        if (bootstrapNode == null) {
//            bootstrapNode = newNode;
//        }
//
//        Message joinMsg = new Message(probe.getRef(), new StatusMsg.Join(bootstrapNode));
//        //Message joinMsg = new Message(probe.getRef(), new StatusMsg.Join(..., joinerId));
//
//        int peers = group.size() - 1;
//        for (var e : group.entrySet()) {
//            if (!e.getKey().equals(joinerId)) {
//                e.getValue().tell(joinMsg);
//            }
//        }
//
//        try {
//            for (int i = 0; i < peers; i++) {
//                //probe.expectMessage(Duration.ofSeconds(3));
//            }
//        } catch (AssertionError ex) {
//            group.remove(joinerId);
//            return false;
//        }
//
//        return true;
        throw new UnsupportedOperationException("Unimplemented");
    }

    /// LEAVE: send StatusMsg.Leave wrapped in Message and wait for LeaveAck
    public boolean leave(int leaverId) {
//        ActorRef<Message> node = group.get(leaverId);
//        if (node == null) return false;
//
//        TestProbe<Message> probe =
//                testKit.createTestProbe(Message.class);
//        Message leaveMsg = new Message(probe.getRef(), new StatusMsg.Leave());
//
//
//        int peers = group.size() - 1;
//        for (var e : group.entrySet()) {
//            if (!e.getKey().equals(leaverId)) {
//                e.getValue().tell(leaveMsg);
//            }
//        }
//
//        try {
//            for (int i = 0; i < peers; i++) {
//                //probe.expectMessage(Duration.ofSeconds(3));
//            }
//            group.remove(leaverId);
//            return true;
//        } catch (AssertionError ex) {
//            group.remove(leaverId);
//            return false;
//        }
        throw new UnsupportedOperationException("Unimplemented");
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