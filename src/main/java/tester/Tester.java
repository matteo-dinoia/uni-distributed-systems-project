package tester;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import messages.Message;
import messages.client.StatusMsg;
import messages.control.ControlMsg;
import messages.node_operation.NodeDataMsg;
import node.NodeActor;
import utils.Pair;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Tester {
    private final ActorTestKit testKit;
    private final Map<Integer, ActorRef<Message>> group = new HashMap<>();

    public Tester(ActorTestKit testKit, Set<Integer> initialNodes) {
        this.testKit = testKit;
        for (Integer id : initialNodes) {
            ActorRef<Message> ref = testKit.spawn(NodeActor.create(), "node" + id);
            group.put(id, ref);
        }
    }

    /**
     * GET+UPDATE: send ReadRequest and Update, wait for ReadResponse and WriteFullyCompleted
     */
    public boolean operationGU(List<Integer> getNodeIds, List<Pair<Integer, String>> updateOps
    ) {
        TestProbe<ControlMsg.ReadResponse> readProbe = testKit.createTestProbe(NodeDataMsg.ReadResponse.class);
        TestProbe<ControlMsg.WriteFullyCompleted> writeProbe = testKit.createTestProbe(ControlMsg.WriteFullyCompleted.class);

        int reqId = 1;
        for (Integer id : getNodeIds) {
            group.get(id).tell(
                    new Message(readProbe.getRef(), new NodeDataMsg.ReadRequest(reqId++, id)
                    )
            );
        }
        for (Pair<Integer, String> op : updateOps) {
            int id = op.getLeft();
            String v = op.getRight();
            group.get(id).tell(new Message(writeProbe.getRef(), new NodeDataMsg.WriteRequest(reqId++, id, v, 0)));
        }

        try {
            for (int i = 0; i < getNodeIds.size(); i++) {
                readProbe.expectMessage(Duration.ofSeconds(3));
            }
            for (int i = 0; i < updateOps.size(); i++) {
                writeProbe.expectMessage(Duration.ofSeconds(3));
            }
            return true;
        } catch (AssertionError ex) {
            return false;
        }
    }

    /**
     * JOIN: send StatusMsg.Join wrapped in Message and wait for JoinAck
     */
    public boolean join(int joinerId) {
        ActorRef<Message> newNode = testKit.spawn(NodeActor.create(), "node" + joinerId);
        group.put(joinerId, newNode);

        TestProbe<ControlMsg.JoinAck> probe =
                testKit.createTestProbe(ControlMsg.JoinAck.class);
        Message joinMsg = new Message(probe.getRef(), new StatusMsg.Join(joinerId, probe.getRef())
        );

        int peers = group.size() - 1;
        for (var e : group.entrySet()) {
            if (!e.getKey().equals(joinerId)) {
                e.getValue().tell(joinMsg);
            }
        }

    public void join(int nodeId) {
//        TestNode node = system.actorOf(NodeActor.props(3), "node" + nodeId);
//        group.put(nodeId, node);
//
//        TestProbe<ControlMsg.NewMemberJoinedAck> probe = testKit.createTestProbe();
//
//        ControlMsg.NewMemberJoined joinedMsg = new ControlMsg.NewMemberJoined(nodeId, probe.getRef());
//
//        for (Map.Entry<Integer, TestNode> entry : group.entrySet()) {
//            if (entry.getKey() != nodeId) {
//                entry.getValue().tell(joinedMsg, null);
//            }
//        }
//
//        int expectedAcks = group.size() - 1;
//        for (int i = 0; i < expectedAcks; i++) {
//            probe.expectMessage(Duration.ofSeconds(3));
//        }
//
//        System.out.println("[Tester] Node " + nodeId + " joined and all peers acknowledged.");
//
//        StatusMsg.InitialMembers initMsg = new StatusMsg.InitialMembers(nodeId, group);
//        node.tell(initMsg, null);
    }


    public void leave() {

    }

    public void recover() {

    }

    public void crash() {

    }

}