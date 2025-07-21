package states;

import akka.actor.ActorRef;
import messages.node_operation.NodeMsg;
import messages.node_operation.NotifyMsg;
import node.DataElement;
import node.Node;
import node.NodeState;
import utils.Config;

import java.util.HashMap;
import java.util.List;

public class Leaving extends AbstractState {
    private final int reqId;
    private final HashMap<Integer, Integer> ackCounts = new HashMap<>();

    public Leaving(Node node) {
        super(node);
        this.reqId = node.getFreshRequestId();
        sendDataLeaving();
        members.scheduleSendTimeoutToMyself(reqId);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.LEAVING;
    }

    private void sendDataLeaving() {
        // TODO MEDIUM only send a single message for each destination (require circular struct or something similar)
        for (Integer key : storage.getAllKeys()) {
            DataElement value = storage.get(key);
            List<ActorRef> newResponsibles = members.findNewResponsiblesFor(key);

            for (ActorRef target : newResponsibles) {
                members.sendTo(target, new NodeMsg.PassResponsabilityRequest(key, value, reqId));
            }

            ackCounts.put(key, 0);
        }
    }

    @Override
    protected AbstractState handlePassResponsabilityResponse(NodeMsg.PassResponsabilityResponse msg) {
        if (msg.requestId() != reqId) return ignore();

        for (Integer key : msg.keys()) {
            ackCounts.put(key, ackCounts.get(key) + 1);
        }

        if (allKeysConfirmed()) {
            members.sendToAll(new NotifyMsg.NodeLeft(members.getSelfId(), members.getSelfRef()));
            return new Left(super.node);
        }

        return keepSameState();
    }

    @Override
    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != reqId) return ignore();

        members.sendToAll(new NodeMsg.RollbackPassResponsability(reqId));
        return new Normal(super.node);
    }

    private boolean allKeysConfirmed() {
        return ackCounts.values().stream().allMatch(count -> count >= Config.W);
    }
}

