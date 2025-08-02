package states;

import actor.NodeState;
import actor.node.Node;
import actor.node.storage.DataElement;
import actor.node.storage.SendableData;
import messages.client.DataMsg;
import messages.client.StatusMsg;
import messages.control.ControlMsg;
import messages.node_operation.NodeDataMsg;
import messages.node_operation.NodeMsg;
import messages.node_operation.NotifyMsg;
import states.sub.Get;
import states.sub.Update;
import utils.Config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

public class Normal extends AbstractState {
    // Map client to its own controller
    public final HashMap<Integer, AbstractState> substates;

    public Normal(Node node) {
        super(node);
        this.substates = new HashMap<>();
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.NORMAL;
    }


    @Override
    public AbstractState handle(Serializable message) {
        return switch (message) {
            // SUBSTATES
            case NodeDataMsg.ReadResponse msg -> handleSubstates(msg, msg.requestId());
            case NodeDataMsg.ReadImpossibleForLock msg -> handleSubstates(msg, msg.requestId());
            case NodeMsg.Timeout msg -> handleSubstates(msg, msg.operationId());
            case NodeDataMsg.WriteLockGranted msg -> handleSubstates(msg, msg.requestId());
            case NodeDataMsg.WriteLockDenied msg -> handleSubstates(msg, msg.requestId());
            case NodeDataMsg.ReadLockAcked msg -> handleSubstates(msg, msg.requestId());
            case NodeDataMsg.WriteAck msg -> handleSubstates(msg, msg.requestId());
            // Ignorable
            case NodeMsg.ResponsabilityResponse _ -> ignore();
            // USE DEFAULT HANDLERS OR 'NORMAL' HANDLER
            default -> super.handle(message);
        };
    }

    @Override
    protected AbstractState handleCrash(StatusMsg.Crash msg) {
        if (!substates.isEmpty())
            return panic("Crashing while still some operation open");

        node.sendTo(sender(), new ControlMsg.CrashAck());
        return new Crashed(super.node);
    }

    @Override
    protected AbstractState handleLeave(StatusMsg.Leave msg) {
        if (!substates.isEmpty())
            return panic("Leaving while still some operation open");

        return Leaving.enter(super.node, sender());
    }

    @Override
    protected AbstractState handleNodeLeft(NotifyMsg.NodeLeft msg) {
        members.removeMember(msg.actorId());
        assert members.size() >= Config.N : "Not enough node left";
        return keepSameState();
    }

    @Override
    protected AbstractState handleNodeJoined(NotifyMsg.NodeJoined msg) {
        members.addMember(msg.actorId(), sender());
        storage.discardNotResponsible(members);
        return keepSameState();
    }


    @Override
    public AbstractState handleUpdate(DataMsg.Update msg) {
        int reqId = node.getFreshRequestId();
        substates.put(reqId, new Update(super.node, sender(), msg, reqId));
        return keepSameState();
    }

    @Override
    public AbstractState handleGet(DataMsg.Get msg) {
        int reqId = node.getFreshRequestId();
        substates.put(reqId, new Get(super.node, sender(), msg, reqId));
        return keepSameState();
    }

    public AbstractState handleSubstates(Serializable msg, int requestId) {
        AbstractState sub = substates.get(requestId);
        if (sub == null)
            return ignore();

        var nextSubstate = sub.handle(sender(), msg);
        if (nextSubstate == null)
            return panic("Substate panic");

        // If the sub state terminated, remove it
        if (nextSubstate.getNodeRepresentation() == NodeState.LEFT)
            substates.remove(requestId);
        return keepSameState();
    }

    @Override
    protected AbstractState handleWriteLockRequest(NodeDataMsg.WriteLockRequest msg) {
        DataElement elem = storage.getOrInsertEmpty(msg.key());

        if (elem.writeLock(sender(), msg.requestId()))
            node.sendTo(sender(), new NodeDataMsg.WriteLockGranted(msg.requestId(), msg.key(), elem.getVersion()));
        else
            node.sendTo(sender(), new NodeDataMsg.WriteLockDenied(msg.requestId()));
        return keepSameState();
    }

    @Override
    protected AbstractState handleLocksRelease(NodeDataMsg.LocksRelease msg) {
        DataElement elem = storage.get(msg.key());
        // For the null case:
        // we can ignore if two are asking to get and we sent to one denied
        // and the other sent back a release before another release from the first
        if (elem == null || !elem.freeLocks(sender(), msg.requestId()))
            return ignore();

        if (elem.getVersion() < 0)
            storage.removeIfRepresentNotExistent(msg.key());
        return keepSameState();

    }

    @Override
    protected AbstractState handleReadLockRequest(NodeDataMsg.ReadLockRequest msg) {
        DataElement elem = storage.get(msg.key());
        if (elem == null || !elem.isWriteLocked())
            return panic("Trying to read lock before write locking");

        boolean couldLock = elem.readLock(sender(), msg.requestId());
        if (!couldLock)
            return panic("Trying to read lock before write locking");

        node.sendTo(sender(), new NodeDataMsg.ReadLockAcked(msg.requestId()));
        return keepSameState();
    }

    @Override
    protected AbstractState handleWriteRequest(NodeDataMsg.WriteRequest msg) {
        DataElement elem = storage.get(msg.key());
        if (elem == null)
            return panic("Writing before read locking");

        boolean wasCorrectlyLocked = elem.freeLocks(sender(), msg.requestId());
        if (!wasCorrectlyLocked)
            return panic("Writing before read locking");

        elem.updateValue(msg.value(), msg.version());
        node.sendTo(sender(), new NodeDataMsg.WriteAck(msg.requestId()));
        return keepSameState();
    }

    @Override
    protected AbstractState handleReadRequest(NodeDataMsg.ReadRequest msg) {
        DataElement elem = storage.get(msg.key());
        if (elem == null)
            elem = new DataElement();

        if (!members.isResponsible(node.actorRef(), msg.key()))
            return panic("Asking for something not under my responsability");
        else if (!elem.isReadLocked())
            node.sendTo(sender(), new NodeDataMsg.ReadResponse(msg.requestId(), msg.key(), elem.sendable()));
        else
            node.sendTo(sender(), new NodeDataMsg.ReadImpossibleForLock(msg.requestId()));
        return keepSameState();
    }

    @Override
    protected AbstractState handleBootstrapRequest(NodeMsg.BootstrapRequest req) {
        var currentMembers = new HashMap<>(members.getMembers());
        node.sendTo(sender(), new NodeMsg.BootstrapResponse(req.requestId(), currentMembers));
        return keepSameState();
    }

    @Override
    protected AbstractState handleResponsabilityRequest(NodeMsg.ResponsabilityRequest msg) {

        HashMap<Integer, SendableData> toSend = new HashMap<>();
        for (Integer key : storage.getAllKeys()) {
            if (members.willBeResponsible(msg.newNodeId(), sender(), key))
                toSend.put(key, storage.get(key).sendable());
        }

        node.sendTo(sender(), new NodeMsg.ResponsabilityResponse(msg.requestId(), node.id(), toSend));
        return keepSameState();
    }

    @Override
    protected AbstractState handlePassResponsabilityRequest(NodeMsg.PassResponsabilityRequest msg) {
        storage.refreshIfNeeded(msg.responsabilities());

        Set<Integer> acks = msg.responsabilities().keySet();
        node.sendTo(sender(), new NodeMsg.PassResponsabilityResponse(msg.requestId(), acks));

        return keepSameState();
    }

    @Override
    protected AbstractState handleRollbackPassResponsability(NodeMsg.RollbackPassResponsability msg) {
        storage.discardNotResponsible(members);
        return keepSameState();
    }


}
