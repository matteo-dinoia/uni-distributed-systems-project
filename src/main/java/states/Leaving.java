package states;

import akka.actor.typed.ActorRef;
import messages.Message;
import messages.control.ControlMsg;
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
    private final ActorRef<Message> mainActorRef;

    private Leaving(Node node, ActorRef<Message> mainActorRef) {
        super(node);
        this.reqId = node.getFreshRequestId();
        this.mainActorRef = mainActorRef;

        sendDataLeaving();
        members.scheduleSendTimeoutToMyself(reqId);
    }

    public static AbstractState enter(Node node, ActorRef<Message> mainActorRef) {
        Leaving leaving = new Leaving(node, mainActorRef);
        if (node.storage().getAllKeys().isEmpty())
            return leaving.concludeLeave();

        return leaving;
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.LEAVING;
    }

    private void sendDataLeaving() {
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
    }

    @Override
    protected AbstractState handlePassResponsabilityResponse(NodeMsg.PassResponsabilityResponse msg) {
        if (msg.requestId() != reqId) return ignore();

        for (Integer key : msg.keys()) {
            ackCounts.put(key, ackCounts.get(key) + 1);
        }

        if (allKeysConfirmed()) {
            return concludeLeave();
        }

        return keepSameState();
    }

    @Override
    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != reqId) return ignore();

        return rollbackLeave();
    }

    private boolean allKeysConfirmed() {
        return ackCounts.values().stream().allMatch(count -> count >= Config.W);
    }

    private AbstractState concludeLeave() {
        members.sendToAll(new NotifyMsg.NodeLeft(members.getSelfId(), members.getSelfRef()));
        members.sendTo(mainActorRef, new ControlMsg.LeaveAck(true));
        return new Left(node);
    }

    private AbstractState rollbackLeave() {
        members.sendToAll(new NodeMsg.RollbackPassResponsability(reqId));
        members.sendTo(mainActorRef, new ControlMsg.LeaveAck(false));
        return new Normal(super.node);
    }
}

