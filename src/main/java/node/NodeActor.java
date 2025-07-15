package node;

import akka.actor.AbstractActor;
import akka.actor.Props;
import states.AbstractState;
import states.Initial;

import java.io.Serializable;

public class NodeActor extends AbstractActor {
    private AbstractState state;
    private final Node node;

    public static Props props(int nodeId) {
        return Props.create(NodeActor.class, () -> new NodeActor(nodeId));
    }

    public NodeActor(int nodeId) {
        this.node = new Node(nodeId, self(), context());
        this.state = new Initial(node);
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
