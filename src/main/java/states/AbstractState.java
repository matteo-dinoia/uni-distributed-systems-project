package states;

import akka.actor.typed.ActorRef;
import messages.Message;
import messages.client.DataMsg;
import messages.client.StatusMsg;
import messages.node_operation.NodeDataMsg;
import messages.node_operation.NodeMsg;
import messages.node_operation.NotifyMsg;
import node.DataStorage;
import node.MemberManager;
import node.Node;
import node.NodeState;

import java.io.Serializable;

// Return new state if it exist (else return this which mean no change)
public abstract class AbstractState {
    protected final MemberManager members;
    protected final DataStorage storage;
    protected final Node node;
    private ActorRef<Message> sender = null;

    protected AbstractState(Node node) {
        this.node = node;
        this.storage = node.storage();
        this.members = node.members();
    }

    // UTILS ---------------------------------------------------------------

    protected AbstractState default_option() {
        return log_unhandled();
    }

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

    protected ActorRef<Message> sender() {
        return this.sender;
    }

    protected void overwriteSender(ActorRef<Message> sender) {
        this.sender = sender;
    }

    public abstract NodeState getNodeRepresentation();

    // DISPATCHER  ---------------------------------------------------------------------

    public final AbstractState handle(ActorRef<Message> sender, Serializable message) {
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
            // ───────────── Notifications ─────────────
            case NotifyMsg.NodeJoined msg -> handleNodeJoined(msg);
            case NotifyMsg.NodeLeft msg -> handleNodeLeft(msg);
            // ───────────── Crash / Leave ─────────────
            case StatusMsg.Crash msg -> handleCrash(msg);
            case StatusMsg.Leave msg -> handleLeave(msg);
            // ───────────── Read (Get) ─────────────
            case DataMsg.Get msg -> handleGet(msg);
            case NodeDataMsg.ReadRequest msg -> handleReadRequest(msg);
            case NodeDataMsg.ReadResponse msg -> handleReadResponse(msg);
            case NodeDataMsg.ReadImpossibleForLock msg -> handleReadImpossibleForLock(msg);
            // ───────────── Write (Update) ─────────────
            case DataMsg.Update msg -> handleUpdate(msg);
            case NodeDataMsg.WriteRequest msg -> handleWriteRequest(msg);
            case NodeDataMsg.WriteAck msg -> handleWriteAck(msg);
            // ───────────── Locking ─────────────
            case NodeDataMsg.WriteLockRequest msg -> handleWriteLockRequest(msg);
            case NodeDataMsg.WriteLockGranted msg -> handleWriteLockGranted(msg);
            case NodeDataMsg.WriteLockDenied msg -> handleWriteLockDenied(msg);
            case NodeDataMsg.WriteLockRelease msg -> handleWriteLockRelease(msg);
            case NodeDataMsg.ReadLockAcked msg -> handleReadLockAcked(msg);
            default -> throw new IllegalStateException("Unexpected message: " + message);
        };
    }


    // MESSAGE HANDLERS ---------------------------------------------------------------
    protected AbstractState handleResponsabilityRequest(NodeMsg.ResponsabilityRequest msg) {
        return default_option();
    }

    protected AbstractState handlePassResponsabilityRequest(NodeMsg.PassResponsabilityRequest msg) {
        return default_option();
    }

    protected AbstractState handleBootstrapRequest(NodeMsg.BootstrapRequest req) {
        return default_option();
    }

    protected AbstractState handleReadRequest(NodeDataMsg.ReadRequest msg) {
        return default_option();
    }

    protected AbstractState handleNodeLeft(NotifyMsg.NodeLeft msg) {
        return default_option();
    }

    protected AbstractState handleNodeJoined(NotifyMsg.NodeJoined msg) {
        return default_option();
    }

    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        return default_option();
    }

    protected AbstractState handleRecover(StatusMsg.Recover msg) {
        return default_option();
    }

    protected AbstractState handleBootstrapResponse(NodeMsg.BootstrapResponse msg) {
        return default_option();
    }

    protected AbstractState handleGet(DataMsg.Get msg) {
        return default_option();
    }

    protected AbstractState handleResponsabilityResponse(NodeMsg.ResponsabilityResponse msg) {
        return default_option();
    }

    protected AbstractState handlePassResponsabilityResponse(NodeMsg.PassResponsabilityResponse msg) {
        return default_option();
    }

    protected AbstractState handleReadResponse(NodeDataMsg.ReadResponse msg) {
        return default_option();
    }

    protected AbstractState handleCrash(StatusMsg.Crash msg) {
        return default_option();
    }

    protected AbstractState handleLeave(StatusMsg.Leave msg) {
        return default_option();
    }


    protected AbstractState handleUpdate(DataMsg.Update msg) {
        return default_option();
    }

    protected AbstractState handleInitialMembers(StatusMsg.InitialMembers msg) {
        return default_option();
    }

    protected AbstractState handleJoin(StatusMsg.Join msg) {
        return default_option();
    }

    protected AbstractState handleWriteLockRequest(NodeDataMsg.WriteLockRequest msg) {
        return default_option();
    }

    protected AbstractState handleWriteLockGranted(NodeDataMsg.WriteLockGranted msg) {
        return default_option();
    }

    protected AbstractState handleWriteLockDenied(NodeDataMsg.WriteLockDenied msg) {
        return default_option();
    }

    protected AbstractState handleWriteLockRelease(NodeDataMsg.WriteLockRelease msg) {
        return default_option();
    }

    protected AbstractState handleWriteRequest(NodeDataMsg.WriteRequest msg) {
        return default_option();
    }

    protected AbstractState handleWriteAck(NodeDataMsg.WriteAck msg) {
        return default_option();
    }

    protected AbstractState handleReadImpossibleForLock(NodeDataMsg.ReadImpossibleForLock msg) {
        return default_option();
    }

    protected AbstractState handleReadLockAcked(NodeDataMsg.ReadLockAcked msg) {
        return default_option();
    }

}




