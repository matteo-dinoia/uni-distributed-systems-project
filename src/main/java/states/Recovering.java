package states;

import akka.actor.ActorRef;
import messages.node_operation.NodeMsg;
import node.Node;
import node.NodeState;

public class Recovering extends AbstractState {
    private final int reqId;

    public Recovering(Node node, ActorRef bootstrapPeer) {
        super(node);
        this.reqId = node.getFreshRequestId();
        sendInitialMsg(bootstrapPeer);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.RECOVERING;
    }

    private void sendInitialMsg(ActorRef bootstrapPeer) {
        members.sendTo(bootstrapPeer, new NodeMsg.BootstrapRequest(reqId));
        members.scheduleSendTimeoutToMyself(reqId);
    }

    @Override
    protected AbstractState handleBootstrapResponse(NodeMsg.BootstrapResponse msg) {
        if (msg.requestId() != reqId)
            return ignore();

        members.setMemberList(msg.updatedMembers());
        storage.discardKeysNotUnderResponsibility(members);
        return new Normal(super.node);
    }

    @Override
    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != reqId)
            return ignore();
        return new Crashed(super.node);
    }

}