package states;

import akka.actor.typed.ActorRef;
import messages.Message;
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
        int requiredAck = sendDataLeaving();
        // TODO case of zero
        members.scheduleSendTimeoutToMyself(reqId);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.LEAVING;
    }

    private int sendDataLeaving() {
        // TODO MEDIUM only send a single message for each destination (require circular struct or something similar)
        HashMap<ActorRef<Message>, HashMap<Integer, DataElement>> new_responsability = new HashMap<>();
        for (Integer key : storage.getAllKeys()) {
            DataElement value = storage.get(key);
            List<ActorRef<Message>> newResponsibles = members.findNewResponsiblesFor(key);

            for (ActorRef<Message> target : newResponsibles) {
                var set = new_responsability.computeIfAbsent(target, ignored -> new HashMap<>());
                set.put(key, value);
            }

            ackCounts.put(key, 0);
        }

        for (var target : new_responsability.keySet()) {
            var list = new_responsability.get(target);
            members.sendTo(target, new NodeMsg.PassResponsabilityRequest(reqId, list));
        }

        return new_responsability.size();
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

