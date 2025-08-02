package states;

import akka.actor.typed.ActorRef;
import messages.Message;
import messages.control.ControlMsg;
import messages.node_operation.NodeMsg;
import messages.node_operation.NotifyMsg;
import node.DataElement;
import node.Node;
import node.NodeState;
import node.SendableData;
import utils.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Leaving extends AbstractState {
    private final int reqId;
    private final HashMap<Integer, Integer> ackCounts = new HashMap<>();
    private final ActorRef<Message> mainActorRef;
    private final Set<ActorRef<Message>> contactedNodes;

    private Leaving(Node node, ActorRef<Message> mainActorRef) {
        super(node);
        this.reqId = node.getFreshRequestId();
        this.mainActorRef = mainActorRef;

        contactedNodes = sendDataLeaving();
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

    private Set<ActorRef<Message>> sendDataLeaving() {
        HashMap<ActorRef<Message>, HashMap<Integer, SendableData>> newResponsability = new HashMap<>();
        for (Integer key : storage.getAllKeys()) {
            DataElement value = storage.get(key);
            List<ActorRef<Message>> newResponsibles = members.findNewResponsiblesFor(key);

            for (ActorRef<Message> target : newResponsibles) {
                var list = newResponsability.computeIfAbsent(target, _ -> new HashMap<>());
                list.put(key, value.sendable());
            }

            ackCounts.put(key, 0);
        }

        for (var target : newResponsability.keySet()) {
            var list = newResponsability.get(target);
            members.sendTo(target, new NodeMsg.PassResponsabilityRequest(reqId, list));
        }

        return newResponsability.keySet();
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
        members.sendToAll(new NotifyMsg.NodeLeft(members.getSelfId()));
        members.sendTo(mainActorRef, new ControlMsg.LeaveAck(true));
        return new Left(node);
    }

    private AbstractState rollbackLeave() {
        members.sendTo(contactedNodes.stream(), new NodeMsg.RollbackPassResponsability(reqId));
        members.sendTo(mainActorRef, new ControlMsg.LeaveAck(false));
        return new Normal(super.node);
    }
}

