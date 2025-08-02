package states;

import actor.NodeState;
import actor.node.Node;
import akka.actor.typed.ActorRef;
import messages.Message;
import messages.control.ControlMsg;
import messages.node_operation.NodeMsg;

import java.io.Serializable;

public class Recovering extends AbstractState {
    private final int reqId;
    private final ActorRef<Message> mainActorRef;

    public Recovering(Node node, ActorRef<Message> mainActorRef, ActorRef<Message> bootstrapPeer) {
        super(node);
        this.reqId = node.getFreshRequestId();
        this.mainActorRef = mainActorRef;
        sendInitialMsg(bootstrapPeer);
    }

    private void sendInitialMsg(ActorRef<Message> bootstrapPeer) {
        node.scheduleTimeout(reqId);
        node.sendTo(bootstrapPeer, new NodeMsg.BootstrapRequest(reqId));
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.RECOVERING;
    }

    // HANDLERS

    @Override
    public AbstractState handle(Serializable message) {
        return switch (message) {
            case NodeMsg.BootstrapResponse msg -> handleBootstrapResponse(msg);
            case NodeMsg.Timeout msg -> handleTimeout(msg);
            default -> log_unhandled(message);
        };
    }

    protected AbstractState handleBootstrapResponse(NodeMsg.BootstrapResponse msg) {
        if (msg.requestId() != reqId)
            return ignore();

        members.setMembers(msg.updatedMembers());
        storage.discardNotResponsible(members);

        node.sendTo(mainActorRef, new ControlMsg.RecoverAck(true));
        return new Normal(super.node);
    }

    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != reqId)
            return ignore();
        node.sendTo(mainActorRef, new ControlMsg.RecoverAck(false));
        return new Crashed(super.node);
    }

}