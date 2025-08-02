package states;

import actor.NodeState;
import actor.node.Node;
import messages.client.StatusMsg;

import java.io.Serializable;

public class Crashed extends AbstractState {
    public Crashed(Node node) {
        super(node);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.CRASHED;
    }

    @Override
    public AbstractState handle(Serializable message) {
        return switch (message) {
            case StatusMsg.Recover msg -> handleRecover(msg);
            default -> ignore();
        };
    }

    @Override
    protected AbstractState handleRecover(StatusMsg.Recover msg) {
        return new Recovering(super.node, sender(), msg.bootstrappingPear());
    }

}
