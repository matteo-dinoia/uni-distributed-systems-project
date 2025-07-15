package states;

import messages.client.StatusMsg;
import node.Node;

import java.io.Serializable;

public class Crashed extends AbstractState {
    public Crashed(Node node) {
        super(node);
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
        return new Recovering(super.node, msg.bootstrappingPear());
    }

}
