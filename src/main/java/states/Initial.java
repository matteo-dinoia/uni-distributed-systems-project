package states;

import actor.NodeState;
import actor.node.Node;
import messages.client.StatusMsg;
import messages.control.ControlMsg;

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

    protected AbstractState handleInitialMembers(StatusMsg.InitialMembers msg) {
        members.setMembers(new HashMap<>(msg.initial()));
        node.sendTo(sender(), new ControlMsg.InitialMembersAck());
        return new Normal(super.node);
    }

    protected AbstractState handleJoin(StatusMsg.Join msg) {
        return new Joining(super.node, msg.bootstrappingPeer(), sender());
    }
}
