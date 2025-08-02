package states;

import actor.NodeState;
import actor.node.MemberManager;
import actor.node.Node;
import actor.node.storage.DataStorage;
import akka.actor.typed.ActorRef;
import messages.Message;
import messages.control.ControlMsg;
import utils.Utils;

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

    @SuppressWarnings("SameReturnValue")
    protected AbstractState panic(String msg) {
        System.err.println("[FATAL] " + msg);
        return null;
    }

    protected AbstractState log_unhandled(Serializable msg) {
        return log("[WARN] Node " + node.id() + " in state " + getNodeRepresentation()
                + " ignoring message '" + msg.getClass().getName() + "' without explicitely declaring so");
    }

    protected AbstractState log(String logValue) {
        System.err.println(logValue);
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

    // ABSTRACT METHODS

    public abstract NodeState getNodeRepresentation();

    protected abstract AbstractState handle(Serializable message);

    // DISPATCHER  ---------------------------------------------------------------------

    public final AbstractState handle(ActorRef<Message> sender, Serializable message) {
        this.sender = sender;

        if (getNodeRepresentation() != NodeState.SUB)
            Utils.debugPrint("==> NODE " + node.id() + " (" + getNodeRepresentation() + ") RECEIVED from " + sender().path().name() + " " + message);

        try {
            return switch (message) {
                // ───────────── Debug (not overwritable) ─────────────
                case ControlMsg.DebugCurrentStateRequest msg -> handleDebugCurrentStateRequest(msg);
                case ControlMsg.DebugCurrentStorageRequest msg -> handleDebugCurrentStorageRequest(msg);
                default -> this.handle(message);
            };
        } catch (RuntimeException | AssertionError e) {
            return panic(e.getMessage());
        }
    }

    // DEBUG MESSAGE HANDLERS ---------------------------------------------------------------

    private AbstractState handleDebugCurrentStateRequest(ControlMsg.DebugCurrentStateRequest ignored) {
        node.sendTo(sender(), new ControlMsg.DebugCurrentStateResponse(getNodeRepresentation()));
        return keepSameState();
    }

    private AbstractState handleDebugCurrentStorageRequest(ControlMsg.DebugCurrentStorageRequest ignored) {
        node.sendTo(sender(), storage.getDebugInfoMsg(node.id()));
        return keepSameState();
    }

}




