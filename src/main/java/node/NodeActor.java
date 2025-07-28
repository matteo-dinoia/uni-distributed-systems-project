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

    public NodeActor(ActorContext<Message> context) {
        super(context);
        this.node = null;
        //this.node = new Node(nodeId, self(), context());
        this.state = new Initial(node);
    }

    public static Behavior<Message> create() {
        return Behaviors.setup(NodeActor::new);
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
