package node;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import messages.Message;
import states.AbstractState;
import states.Initial;

public class NodeActor extends AbstractBehavior<Message> {
    private AbstractState state;
    private final Node node;

    public NodeActor(ActorContext<Message> context, int nodeId, AbstractState state) {
        super(context);
        this.node = new Node(nodeId, context);

        this.state = state != null ? state : new Initial(node);
    }

    public static Behavior<Message> create(int nodeId) {
        return create(nodeId, null);
    }

    public static Behavior<Message> create(int nodeId, AbstractState state) {
        return Behaviors.setup(context -> new NodeActor(context, nodeId, state));
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder().onAnyMessage(this::handle).build();
    }

    private Behavior<Message> handle(Message msg) {
        var nextState = state.handle(msg.sender(), msg.content());

        if (nextState == null) {
            System.out.println("PANIC on node " + node.members().getSelfId());
        } else {
            if (!this.state.getNodeRepresentation().isValidChange(nextState.getNodeRepresentation()))
                System.out.println("INVALID STATE TRANSACTION in" + node.members().getSelfId());
            this.state = nextState;
        }

        return this;
    }

}
