package tester;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.typed.ActorRef;
import messages.Message;
import messages.client.StatusMsg;
import node.NodeActor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class Tester {
//    private record TestNode(ActorRef ref, TestProbe<Message> probe) {
//    }

    private final HashMap<Integer, ActorRef<Message>> group = new HashMap<>();

    public Tester(ActorTestKit testKit, HashSet<Integer> node_indexes) {

        // add members to group
        for (Integer node_i : node_indexes) {
            if (node_i == null) continue;
//            var testProbe = testKit.createTestProbe(Message.class);
            var ref = testKit.spawn(NodeActor.create());
            group.put(node_i, ref);
        }

        // send the group member list to everyone in the group
        StatusMsg.InitialMembers initialMsg = new StatusMsg.InitialMembers(1, Collections.unmodifiableMap(group));
        for (var actorRef : group.values())
            actorRef.tell(new Message(null, initialMsg));
    }


    public void operationGU() {

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