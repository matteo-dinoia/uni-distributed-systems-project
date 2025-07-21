package states;

import akka.actor.ActorRef;
import messages.node_operation.NodeMsg;
import messages.node_operation.NotifyMsg;
import node.DataElement;
import node.Node;
import node.NodeState;
import utils.Config;
import utils.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class Joining extends AbstractState {
    // key: (data, how many replicas confirmed it)
    private final HashMap<Integer, Pair<DataElement, Integer>> receivedData = new HashMap<>();
    private final Set<ActorRef> responded = new HashSet<>();
    private final int reqId;

    private Integer closestHigherResponded = null;
    private Integer closestLowerResponded = null;


    public Joining(Node node) {
        super(node);
        this.reqId = node.getFreshRequestId();
        sendInitialMsg();
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.JOINING;
    }

    // TODO HARD It is probably wrong if too little elements are in there
    private void sendInitialMsg() {
        members.sendTo2n(new NodeMsg.ResponsabilityRequest(reqId, members.getSelfRef()));
        members.scheduleSendTimeoutToMyself(reqId);
    }

    @Override
    protected AbstractState handleResponsabilityResponse(NodeMsg.ResponsabilityResponse msg) {
        if (msg.requestId() != reqId)
            return ignore();

        boolean enough_responded = addResponded(sender(), msg.senderId());
        boolean enough_quorum = addData(msg.data());

        if (enough_responded && enough_quorum) {
            // save into storage
            for (var entry : receivedData.entrySet())
                storage.put(entry.getKey(), entry.getValue().getLeft());

            members.sendToAll(new NotifyMsg.NodeJoined(members.getSelfId(), members.getSelfRef()));
            return new Normal(super.node);
        }

        return keepSameState();
    }

    @Override
    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != reqId)
            return ignore();
        return new Initial(super.node);
    }

    // TODO HARD It is probably wrong if too little elements are in there
    private boolean addResponded(ActorRef sender, int senderId) {
        int selfId = members.getSelfId();
        responded.add(sender());

        if (closestLowerResponded == null || members.isCloserCounterClockwise(senderId, closestLowerResponded, selfId)) {
            closestLowerResponded = senderId;
        }

        if (closestHigherResponded == null || members.isCloserClockwise(senderId, closestHigherResponded, selfId)) {
            closestHigherResponded = senderId;
        }

        return members.countMembersBetweenIncluded(closestHigherResponded, closestLowerResponded) <= Config.N;
    }

    private boolean addData(HashMap<Integer, DataElement> data) {
        for (var entry : data.entrySet()) {
            int key = entry.getKey();
            DataElement new_value = entry.getValue();

            Pair<DataElement, Integer> pair = receivedData.computeIfAbsent(key, _ -> new Pair<>(new_value, 0));
            pair.setRight(pair.getRight() + 1);
            if (pair.getLeft().getVersion() < new_value.getVersion()) {
                pair.setLeft(new_value);
            }
        }

        return hasSufficientReplicas();
    }

    private boolean hasSufficientReplicas() {
        for (Pair<DataElement, Integer> pair : receivedData.values()) {
            if (pair.getRight() < Config.R) {
                return false;
            }
        }
        return true;
    }
}
