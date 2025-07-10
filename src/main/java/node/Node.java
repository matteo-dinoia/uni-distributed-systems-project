package node;

import akka.actor.AbstractActor;
import akka.actor.Props;
import states.AbstractState;

import java.io.Serializable;

public class Node extends AbstractActor {
    private AbstractState state;
    private final MemberManager memberManager;
    private final DataStorage datas;

    public static Props props(int nodeId) {
        return Props.create(Node.class, () -> new Node(nodeId));
    }

    public Node(int nodeId) {
        this.memberManager = new MemberManager(nodeId, self());
        this.datas = new DataStorage();
        this.state = null;
    }

    // TODO
    @Override
    public Receive createReceive() {
        return receiveBuilder().match(Serializable.class, this::handle)
                .build();
    }

    private void handle(Serializable msg) {
        if (state == null) {
            // TODO do something
        }

        var nextState = state.handle(sender(), msg);
        this.state = nextState;
    }

}
