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

    public NodeActor(ActorContext<Message> context, int nodeId) {
        super(context);
        this.node = new Node(nodeId, context);
        this.state = new Initial(node);
    }

    public static Behavior<Message> create(int nodeId) {
        return Behaviors.setup(context -> new NodeActor(context, nodeId));
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
            NodeState curr = this.state.getNodeRepresentation();
            NodeState next = nextState.getNodeRepresentation();
            if (!curr.isValidChange(next))
                System.out.println("INVALID STATE TRANSACTION in node " + node.members().getSelfId() +
                        " from " + curr + " to " + next);
            else if (curr == next)
                System.out.println("STATE DIDN'T CHANGE in node " + node.members().getSelfId() +
                        " and is " + curr);
            else
                System.out.println("STATE CHANGED in node " + node.members().getSelfId() +
                        " from " + curr + " to " + next);
            this.state = nextState;
        }

        return this;
    }

}
