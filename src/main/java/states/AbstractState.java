package states;

import akka.actor.typed.ActorRef;
import messages.Message;
import messages.client.DataMsg;
import messages.client.StatusMsg;
import messages.control.ControlMsg;
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

    protected AbstractState default_option(Serializable msg) {
        return log_unhandled(msg);
    }

    @SuppressWarnings("SameReturnValue")
    protected AbstractState panic() {
        System.out.println("[FATAL] Something catastrofic happened");
        return null;
    }

    protected AbstractState log_unhandled(Serializable msg) {
        return log("[WARN] Node " + members.getSelfId() + " in state " + getNodeRepresentation()
                + " ignoring message '" + msg.getClass().getName() + "' without explicitely declaring so");
    }

    protected AbstractState log(String logValue) {
        System.out.println(logValue);
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

    private void overwriteSender(ActorRef<Message> sender) {
        this.sender = sender;
    }

    public abstract NodeState getNodeRepresentation();

    // DISPATCHER  ---------------------------------------------------------------------

    public final AbstractState handle(ActorRef<Message> sender, Serializable message) {
        overwriteSender(sender);
        System.out.println("==> NODE " + members.getSelfId() + " RECEIVED  " + message.toString());

        return switch (message) {
            // ───────────── Debug (not overwritable) ─────────────
            case ControlMsg.DebugCurrentStateRequest msg -> handleDebugCurrentStateRequest(msg);
            case ControlMsg.DebugCurrentStorageRequest msg -> handleDebugCurrentStorageRequest(msg);
            default -> this.handle(message);
        };
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
            case NodeMsg.RollbackPassResponsability msg -> handleRollbackPassResponsability(msg);
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
            case NodeDataMsg.ReadLockRequest msg -> handleReadLockRequest(msg);
            default -> throw new IllegalStateException("Unexpected message: " + message);
        };
    }

    // CUSTOM MESSAGE HANDLERS ---------------------------------------------------------------
    private AbstractState handleDebugCurrentStateRequest(ControlMsg.DebugCurrentStateRequest ignored) {
        members.sendTo(sender(), new ControlMsg.DebugCurrentStateResponse(getNodeRepresentation()));
        return keepSameState();
    }

    private AbstractState handleDebugCurrentStorageRequest(ControlMsg.DebugCurrentStorageRequest ignored) {
        int nodeId = members.getSelfId();
        members.sendTo(sender(), new ControlMsg.DebugCurrentStorageResponse(nodeId, storage.getCopyOfData()));
        return keepSameState();
    }

    protected AbstractState handleReadResponse(NodeDataMsg.ReadResponse msg) {
        return ignore();
    }


    // MESSAGE HANDLERS ---------------------------------------------------------------
    protected AbstractState handleRollbackPassResponsability(NodeMsg.RollbackPassResponsability msg) {
        return default_option(msg);
    }

    protected AbstractState handleResponsabilityRequest(NodeMsg.ResponsabilityRequest msg) {
        return default_option(msg);
    }

    protected AbstractState handlePassResponsabilityRequest(NodeMsg.PassResponsabilityRequest msg) {
        return default_option(msg);
    }

    protected AbstractState handleReadLockRequest(NodeDataMsg.ReadLockRequest msg) {
        return default_option(msg);
    }

    protected AbstractState handleBootstrapRequest(NodeMsg.BootstrapRequest msg) {
        return default_option(msg);
    }

    protected AbstractState handleReadRequest(NodeDataMsg.ReadRequest msg) {
        return default_option(msg);
    }

    protected AbstractState handleNodeLeft(NotifyMsg.NodeLeft msg) {
        return default_option(msg);
    }

    protected AbstractState handleNodeJoined(NotifyMsg.NodeJoined msg) {
        return default_option(msg);
    }

    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        return default_option(msg);
    }

    protected AbstractState handleRecover(StatusMsg.Recover msg) {
        return default_option(msg);
    }

    protected AbstractState handleBootstrapResponse(NodeMsg.BootstrapResponse msg) {
        return default_option(msg);
    }

    protected AbstractState handleGet(DataMsg.Get msg) {
        return default_option(msg);
    }

    protected AbstractState handleResponsabilityResponse(NodeMsg.ResponsabilityResponse msg) {
        return default_option(msg);
    }

    protected AbstractState handlePassResponsabilityResponse(NodeMsg.PassResponsabilityResponse msg) {
        return default_option(msg);
    }


    protected AbstractState handleCrash(StatusMsg.Crash msg) {
        return default_option(msg);
    }

    protected AbstractState handleLeave(StatusMsg.Leave msg) {
        return default_option(msg);
    }


    protected AbstractState handleUpdate(DataMsg.Update msg) {
        return default_option(msg);
    }

    protected AbstractState handleInitialMembers(StatusMsg.InitialMembers msg) {
        return default_option(msg);
    }

    protected AbstractState handleJoin(StatusMsg.Join msg) {
        return default_option(msg);
    }

    protected AbstractState handleWriteLockRequest(NodeDataMsg.WriteLockRequest msg) {
        return default_option(msg);
    }

    protected AbstractState handleWriteLockGranted(NodeDataMsg.WriteLockGranted msg) {
        return default_option(msg);
    }

    protected AbstractState handleWriteLockDenied(NodeDataMsg.WriteLockDenied msg) {
        return default_option(msg);
    }

    protected AbstractState handleWriteLockRelease(NodeDataMsg.WriteLockRelease msg) {
        return default_option(msg);
    }

    protected AbstractState handleWriteRequest(NodeDataMsg.WriteRequest msg) {
        return default_option(msg);
    }

    protected AbstractState handleWriteAck(NodeDataMsg.WriteAck msg) {
        return default_option(msg);
    }

    protected AbstractState handleReadImpossibleForLock(NodeDataMsg.ReadImpossibleForLock msg) {
        return default_option(msg);
    }

    protected AbstractState handleReadLockAcked(NodeDataMsg.ReadLockAcked msg) {
        return default_option(msg);
    }

}




