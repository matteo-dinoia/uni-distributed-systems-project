package states;

import akka.actor.ActorRef;
import messages.client.DataMsg;
import messages.client.StatusMsg;
import messages.node_operation.NodeDataMsg;
import messages.node_operation.NodeMsg;
import messages.node_operation.NotifyMsg;
import node.*;

import java.io.Serializable;
import java.util.HashMap;

// Return new state if it exist (else return this which mean no change)
public abstract class AbstractState {
    protected final MemberManager members;
    protected final DataStorage storage;
    protected final Node node;
    private ActorRef sender = null;

    protected AbstractState(Node node) {
        this.node = node;
        this.storage = node.storage();
        this.members = node.members();
    }

    // UTILS ---------------------------------------------------------------

    protected AbstractState panic() {
        System.out.println("Something catastrofic happened");
        System.exit(1);
        return null;
    }

    protected AbstractState log_unhandled() {
        System.out.println("Something not handled explicitly happened");
        return this;
    }

    protected AbstractState ignore() {
        return this;
    }

    protected AbstractState keepSameState() {
        return this;
    }

    protected ActorRef sender() {
        return this.sender;
    }

    protected void overwriteSender(ActorRef sender) {
        this.sender = sender;
    }

    public abstract NodeState getNodeRepresentation();

    // DISPATCHER  ---------------------------------------------------------------------

    public final AbstractState handle(ActorRef sender, Serializable message) {
        overwriteSender(sender);
        return this.handle(message);
    }

    // Overriding this ignore all default handler
    protected AbstractState handle(Serializable message) {
        return switch (message) {
            // ───────────── Join ─────────────
            case StatusMsg.Join msg -> handleJoin(msg);
            case StatusMsg.InitialMembers msg -> handleInitialMembers(msg);
            case NodeMsg.BootstrapRequest msg -> handleBootstrapRequest(msg);
            case NodeMsg.BootstrapResponse msg -> handleBootstrapResponse(msg);
            case NodeMsg.ResponsabilityRequest msg -> handleResponsabilityRequest(msg);
            case NodeMsg.ResponsabilityResponse msg -> handleResponsabilityResponse(msg);
            // ───────────── Leave ─────────────
            case NodeMsg.PassResponsabilityRequest msg -> handlePassResponsabilityRequest(msg);
            case NodeMsg.PassResponsabilityResponse msg -> handlePassResponsabilityResponse(msg);
            // ───────────── Recover ─────────────
            case StatusMsg.Recover msg -> handleRecover(msg);
            // ───────────── Timeout ─────────────
            case NodeMsg.Timeout msg -> handleTimeout(msg);
            // ───────────── Normal ─────────────
            case NotifyMsg.NodeJoined msg -> handleNodeJoined(msg);
            case NotifyMsg.NodeLeft msg -> handleNodeLeft(msg);
            case StatusMsg.Crash msg -> handleCrash(msg);
            case StatusMsg.Leave msg -> handleLeave(msg);
            // TODO handle all the msgs (check for more)
            // ───────────── Read (Get) ─────────────
            case DataMsg.Get msg -> handleGet(msg); //TODO inside normal hashmap of operations.
            case NodeDataMsg.ReadRequest msg -> handleReadRequest(msg);
            case NodeDataMsg.ReadReply msg -> handleReadReply(msg);
            // ───────────── Write (Update) ─────────────
            // TODO: add handling for DataMsg.UpdateMsg and relevant internal messages (WriteRequest, WriteAck)
            case DataMsg.Update msg -> handleUpdate(msg);
            // case DataMsg.UpdateMsg msg -> handleUpdate(msg);
            // case NodeMsg.WriteAck msg -> handleWriteAck(msg);
            default -> throw new IllegalStateException("Unexpected value: " + message);
        };
    }


    // MESSAGE HANDLERS ---------------------------------------------------------------

    // Custom handler

    protected AbstractState handleAskForMyResponsibility(NodeMsg.ResponsabilityRequest msg) {
        HashMap<Integer, DataElement> toSend = new HashMap<>();
        for (Integer key : storage.getAllKeys()) {
            if (members.isResponsible(msg.requester(), key)) {
                toSend.put(key, storage.get(key));
            }
        }

        members.sendTo(msg.requester(), new NodeMsg.ResponsabilityResponse(msg.requestId(), members.getSelfId(), toSend));
        return keepSameState();
    }

    protected AbstractState handleBootstrapRequest(NodeMsg.BootstrapRequest req) {
        HashMap<Integer, ActorRef> currentMembers = members.getMemberList();
        members.sendTo(sender(), new NodeMsg.BootstrapResponse(req.requestId(), currentMembers));
        return keepSameState();
    }

    protected AbstractState handleReadRequest(NodeDataMsg.ReadRequest msg) {
        DataElement element = storage.get(msg.key());
        members.sendTo(sender(), new NodeDataMsg.ReadReply(msg.requestId(), element));
        return keepSameState();
    }

    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        return ignore();
    }

    // Base responses

    protected AbstractState handleRecover(StatusMsg.Recover msg) {
        return panic();
    }

    protected AbstractState handleBootstrapResponse(NodeMsg.BootstrapResponse msg) {
        return panic();
    }

    protected AbstractState handleGet(DataMsg.Get msg) {
        return log_unhandled();
    }

    protected AbstractState handleResponsabilityRequest(NodeMsg.ResponsabilityRequest msg) {
        return panic();
    }

    protected AbstractState handleResponsabilityResponse(NodeMsg.ResponsabilityResponse msg) {
        return panic();
    }

    protected AbstractState handlePassResponsabilityRequest(NodeMsg.PassResponsabilityRequest msg) {
        return panic();
    }

    protected AbstractState handlePassResponsabilityResponse(NodeMsg.PassResponsabilityResponse msg) {
        return panic();
    }

    protected AbstractState handleReadReply(NodeDataMsg.ReadReply msg) {
        return panic();
    }

    protected AbstractState handleCrash(StatusMsg.Crash msg) {
        return panic();
    }

    protected AbstractState handleLeave(StatusMsg.Leave msg) {
        return panic();
    }

    protected AbstractState handleNodeLeft(NotifyMsg.NodeLeft msg) {
        return panic();
    }

    protected AbstractState handleNodeJoined(NotifyMsg.NodeJoined msg) {
        return panic();
    }

    protected AbstractState handleUpdate(DataMsg.Update msg) {
        return panic();
    }

    protected AbstractState handleInitialMembers(StatusMsg.InitialMembers msg) {
        return panic();
    }

    protected AbstractState handleJoin(StatusMsg.Join msg) {
        return panic();
    }

}




