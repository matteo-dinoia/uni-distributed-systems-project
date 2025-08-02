package states;

import actor.NodeState;
import actor.node.Node;

import java.io.Serializable;

public class Left extends AbstractState {
    public Left(Node node) {
        super(node);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.LEFT;
    }

    @Override
    public AbstractState handle(Serializable message) {
        return ignore();
    }
}