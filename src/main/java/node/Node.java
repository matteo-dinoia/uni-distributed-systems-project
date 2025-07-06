package node;

import akka.actor.AbstractActor;
import akka.actor.Props;
import messages.ClientMsgs;

import java.util.HashMap;

public class Node extends AbstractActor {
    private final MemberManager memberManager;
    // Map operationId to its own controller
    private final HashMap<Integer, Actionator> actionators;
    // Map key to value and its own operator
    private final HashMap<Integer, DataOperator> data;

    public Node() {
        memberManager = new MemberManager(getSelf());
        actionators = new HashMap<>();
        data = new HashMap<>();
    }

    public static Props props() {
        return Props.create(Node.class, Node::new);
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ClientMsgs.DataMsg.class, this::passToDataOperator)
                .match(ClientMsgs.StatusMsg.class, this::passToMemberManager)
                .build();
    }

    void passToDataOperator(ClientMsgs.DataMsg msg) {
        DataOperator dataOp = data.putIfAbsent(msg.key, new DataOperator(this.memberManager));
        assert dataOp != null;
        dataOp.handle(msg);
    }

    void passToMemberManager(ClientMsgs.StatusMsg msg) {
        memberManager.handle(msg);
    }
}
