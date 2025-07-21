package states;

import akka.actor.ActorRef;
import messages.client.DataMsg;
import messages.client.StatusMsg;
import messages.node_operation.NodeMsg;
import messages.node_operation.NotifyMsg;
import node.DataElement;
import node.Node;
import node.NodeState;
import states.sub.Get;
import states.sub.Update;

import java.io.Serializable;
import java.util.HashMap;

public class Normal extends AbstractState {
    // Map client to its own controller
    public HashMap<ActorRef, AbstractState> substates;

    public Normal(Node node) {
        super(node);
        this.substates = new HashMap<>();
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.ALIVE;
    }

    @Override
    public AbstractState handle(Serializable message) {
        return switch (message) {
            case StatusMsg.Crash msg -> handleCrash(msg);
            case StatusMsg.Leave msg -> handleLeave(msg);
            case NotifyMsg.NodeJoined msg -> handleNodeJoined(msg);
            case NotifyMsg.NodeLeft msg -> handleNodeLeft(msg);
            case DataMsg.Get msg -> handleGet(msg);
            case DataMsg.Update msg -> handleUpdate(msg);
            default -> handleSubstates(message);
        };
    }

    @Override
    protected AbstractState handleCrash(StatusMsg.Crash msg) {
        if (!substates.isEmpty()) {
            return panic();
        }
        return new Crashed(super.node);
    }

    @Override
    protected AbstractState handleLeave(StatusMsg.Leave msg) {
        if (!substates.isEmpty()) {
            return panic();
        }
        return new Leaving(super.node);
    }

    @Override
    protected AbstractState handleNodeLeft(NotifyMsg.NodeLeft msg) {
        // TODO EASY
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    protected AbstractState handleNodeJoined(NotifyMsg.NodeJoined msg) {
        // TODO EASY
        throw new UnsupportedOperationException("Not implemented yet");
    }


    @Override
    public AbstractState handleUpdate(DataMsg.Update msg) {
        AbstractState sub = substates.get(sender());
        if (sub != null)
            return panic();

        substates.put(sender(), new Update(super.node, sender(), msg));
        return keepSameState();
    }

    @Override
    public AbstractState handleGet(DataMsg.Get msg) {
        AbstractState sub = substates.get(sender());
        if (sub != null)
            return panic();
        substates.put(sender(), new Get(super.node, sender(), msg));
        return keepSameState();
    }


    public AbstractState handleSubstates(Serializable msg) {
        AbstractState sub = substates.get(sender());
        if (sub == null)
            return ignore();

        sub.overwriteSender(sender());
        var nextSubstate = sub.handle(msg);
        if (nextSubstate == null)
            return panic();

        if (nextSubstate.getNodeRepresentation() == NodeState.ALIVE)
            substates.remove(sender());
        return keepSameState();
    }


    protected AbstractState handleLockRequest(NodeMsg.LockRequest msg) {
        DataElement elem = storage.get(msg.key());
        if (elem == null)
            return panic();

        if (elem.isLockedForWrite()) {
            members.sendTo(sender(), new NodeMsg.LockDenied(msg.requestId()));
        } else {
            elem.setLockedForWrite(true);
            members.sendTo(sender(), new NodeMsg.LockGranted(msg.requestId(), elem));
        }
        return keepSameState();
    }

    protected AbstractState handleLockRelease(NodeMsg.LockRelease msg) {
        DataElement elem = storage.get(msg.key());
        if (elem != null) elem.setLockedForWrite(false);
        return keepSameState();
    }

}
