package states;

import akka.actor.typed.ActorRef;
import messages.Message;
import messages.control.ControlMsg;
import messages.node_operation.NodeMsg;
import node.Node;
import node.NodeState;

public class Recovering extends AbstractState {
    private final int reqId;
    private final ActorRef<Message> mainActorRef;

    public Recovering(Node node, ActorRef<Message> mainActorRef, ActorRef<Message> bootstrapPeer) {
        super(node);
        this.reqId = node.getFreshRequestId();
        this.mainActorRef = mainActorRef;
        sendInitialMsg(bootstrapPeer);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.RECOVERING;
    }

    private void sendInitialMsg(ActorRef<Message> bootstrapPeer) {
        node.sendTo(bootstrapPeer, new NodeMsg.BootstrapRequest(reqId));
        node.scheduleTimeout(reqId);
    }

    @Override
    protected AbstractState handleBootstrapResponse(NodeMsg.BootstrapResponse msg) {
        if (msg.requestId() != reqId)
            return ignore();

        members.setMembers(msg.updatedMembers());
        storage.discardKeysNotUnderResponsibility(members);

        node.sendTo(mainActorRef, new ControlMsg.RecoverAck(true));
        return new Normal(super.node);
    }

    @Override
    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != reqId)
            return ignore();
        node.sendTo(mainActorRef, new ControlMsg.RecoverAck(false));
        return new Crashed(super.node);
    }

}