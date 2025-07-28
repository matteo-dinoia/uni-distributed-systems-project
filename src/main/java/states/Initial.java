package states;

import messages.client.StatusMsg;
import node.Node;
import node.NodeState;

import java.io.Serializable;
import java.util.HashMap;

public class Initial extends AbstractState {
    public Initial(Node node) {
        super(node);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.TO_START;
    }

    @Override
    public AbstractState handle(Serializable message) {
        return switch (message) {
            case StatusMsg.Join msg -> handleJoin(msg);
            case StatusMsg.InitialMembers msg -> handleInitialMembers(msg);
            default -> ignore();
        };
    }

    @Override
    protected AbstractState handleInitialMembers(StatusMsg.InitialMembers msg) {
        members.setMemberList(new HashMap<>(msg.initial()));
        return new Normal(super.node);
    }

    @Override
    protected AbstractState handleJoin(StatusMsg.Join msg) {
        return new Joining(super.node);
    }
}
