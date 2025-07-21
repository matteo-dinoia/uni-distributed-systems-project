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

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(Serializable.class, this::handle)
                .build();
    }

    private void handle(Serializable msg) {
        var nextState = state.handle(sender(), msg);

        if (nextState == null) {
            // TODO MAYBE remove panics
            System.out.println("PANIC on node " + node.members().getSelfId());
        } else {
            if (!this.state.getNodeRepresentation().isValidChange(nextState.getNodeRepresentation()))
                System.out.println("INVALID STATE TRANSACTION in" + node.members().getSelfId());
            this.state = nextState;
        }

    }

}
