package states;

import messages.client.StatusMsg;
import node.Node;

import java.io.Serializable;

public class Initial extends AbstractState {
    public Initial(Node node) {
        super(node);
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
        members.setMemberList(msg.initial());
        return new Normal(super.node);
    }

    @Override
    protected AbstractState handleJoin(StatusMsg.Join msg) {
        return new Joining(super.node);
    }
}
