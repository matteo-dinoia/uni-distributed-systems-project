package states;

import akka.actor.typed.ActorRef;
import messages.Message;
import messages.client.DataMsg;
import messages.client.StatusMsg;
import messages.control.ControlMsg;
import messages.node_operation.NodeDataMsg;
import messages.node_operation.NodeMsg;
import messages.node_operation.NotifyMsg;
import node.DataElement;
import node.Node;
import node.NodeState;
import states.sub.Get;
import states.sub.Update;
import utils.Config;

import java.io.Serializable;
import java.util.HashMap;

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
            // USE DEFAULT HANDLERS OR 'NORMAL' HANDLER
            default -> super.handle(message);
        };
    }

    @Override
    protected AbstractState handleCrash(StatusMsg.Crash msg) {
        if (!substates.isEmpty()) {
            return panic();
        }
        members.sendTo(sender(), new ControlMsg.CrashAck());
        return new Crashed(super.node);
    }

    @Override
    protected AbstractState handleLeave(StatusMsg.Leave msg) {
        if (!substates.isEmpty()) {
            return panic();
        }
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
        members.addMember(msg.actorId(), msg.actorRef());
        storage.removeNotUnderMyControl(members);
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
            return log("[WARN] Substate request was ignored");

        var nextSubstate = sub.handle(sender(), msg);
        if (nextSubstate == null)
            return panic();

        if (nextSubstate.getNodeRepresentation() == NodeState.NORMAL)
            substates.remove(requestId);
        return keepSameState();
    }

    @Override
    protected AbstractState handleWriteLockRequest(NodeDataMsg.WriteLockRequest msg) {
        DataElement elem = storage.get(msg.key());
        if (elem == null) {
            elem = new DataElement();
            storage.put(msg.key(), elem);
        }


        if (elem.isWriteLocked()) {
            members.sendTo(sender(), new NodeDataMsg.WriteLockDenied(msg.requestId()));
        } else {
            elem.setWriteLocked(true);
            members.sendTo(sender(), new NodeDataMsg.WriteLockGranted(msg.requestId(), elem.getVersion()));
        }
        return keepSameState();
    }

    @Override
    protected AbstractState handleWriteLockRelease(NodeDataMsg.WriteLockRelease msg) {
        DataElement elem = storage.get(msg.key());
        if (elem == null)
            return panic();
        else if (elem.getVersion() < 0)
            storage.removeIfRepresentNotExistent(msg.key());

        elem.setWriteLocked(false);
        return keepSameState();
    }

    @Override
    protected AbstractState handleReadLockRequest(NodeDataMsg.ReadLockRequest msg) {
        DataElement elem = storage.get(msg.key());
        if (elem == null || !elem.isWriteLocked())
            return panic();

        elem.setReadLocked(true);
        members.sendTo(sender(), new NodeDataMsg.ReadLockAcked(msg.requestId()));
        return keepSameState();
    }

    @Override
    protected AbstractState handleWriteRequest(NodeDataMsg.WriteRequest msg) {
        DataElement elem = storage.get(msg.key());
        if (elem == null)
            return panic();

        elem.updateValue(msg.value(), msg.version());
        elem.setWriteLocked(false);
        elem.setReadLocked(false);

        members.sendTo(sender(), new NodeDataMsg.WriteAck(msg.requestId()));
        return keepSameState();
    }

    @Override
    protected AbstractState handleReadRequest(NodeDataMsg.ReadRequest msg) {

        DataElement elem = storage.get(msg.key());
        if (elem == null)
            elem = new DataElement();

        if (!elem.isReadLocked())
            members.sendTo(sender(), new NodeDataMsg.ReadResponse(msg.requestId(), elem));
        else if (!members.isResponsible(members.getSelfRef(), msg.key()))
            return panic();
        else
            members.sendTo(sender(), new NodeDataMsg.ReadImpossibleForLock(msg.requestId()));
        return keepSameState();
    }

    @Override
    protected AbstractState handleBootstrapRequest(NodeMsg.BootstrapRequest req) {
        HashMap<Integer, ActorRef<Message>> currentMembers = members.getMemberList();
        members.sendTo(sender(), new NodeMsg.BootstrapResponse(req.requestId(), currentMembers));
        return keepSameState();
    }

    @Override
    protected AbstractState handleResponsabilityRequest(NodeMsg.ResponsabilityRequest msg) {
        HashMap<Integer, DataElement> toSend = new HashMap<>();
        for (Integer key : storage.getAllKeys()) {
            if (members.isResponsible(msg.requester(), key)) {
                toSend.put(key, storage.get(key));
            }
        }

        members.sendTo(msg.requester(), new NodeMsg.ResponsabilityResponse(msg.requestId(), members.getSelfId(), toSend));
        return keepSameState();
    }

    @Override
    protected AbstractState handlePassResponsabilityRequest(NodeMsg.PassResponsabilityRequest msg) {
        storage.putAll(msg.responsabilities());
        members.sendTo(sender(), new NodeMsg.PassResponsabilityResponse(msg.requestId(), msg.responsabilities().keySet()));
        return keepSameState();
    }

    @Override
    protected AbstractState handleRollbackPassResponsability(NodeMsg.RollbackPassResponsability msg) {
        storage.removeNotUnderMyControl(members);
        return keepSameState();
    }


}
