package states;

import akka.actor.typed.ActorRef;
import messages.Message;
import messages.control.ControlMsg;
import messages.node_operation.NodeMsg;
import messages.node_operation.NotifyMsg;
import node.Node;
import node.NodeState;
import node.SendableData;
import utils.Config;
import utils.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implements node join in two phases:
 * 1) BOOTSTRAP: ask a peer for the current member list
 * 2) RESPONSIBILITIES: request data for keys this node will now serve
 */
public class Joining extends AbstractState {
    private final ActorRef<Message> mainActorRef;

    private enum JoinPhase {BOOTSTRAP, RESPONSIBILITIES}

    private JoinPhase phase;

    // key: (data, how many replicas confirmed it)
    private final HashMap<Integer, Pair<SendableData, Integer>> receivedData = new HashMap<>();
    private final Set<ActorRef<Message>> responded = new HashSet<>();
    private final int reqId;

    private Integer closestHigherResponded = null;
    private Integer closestLowerResponded = null;

    /**
     * @param bootstrapPeer a live node to bootstrap membership from
     */
    public Joining(Node node, ActorRef<Message> bootstrapPeer, ActorRef<Message> mainActorRef) {
        super(node);
        this.reqId = node.getFreshRequestId();
        this.phase = JoinPhase.BOOTSTRAP;
        this.mainActorRef = mainActorRef;
        members.sendTo(bootstrapPeer, new NodeMsg.BootstrapRequest(reqId));
        members.scheduleSendTimeoutToMyself(reqId);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.JOINING;
    }

    @Override
    protected AbstractState handleBootstrapResponse(NodeMsg.BootstrapResponse msg) {
        if (msg.requestId() != reqId || phase != JoinPhase.BOOTSTRAP)
            return ignore();

        members.setMemberList(msg.updatedMembers());
        phase = JoinPhase.RESPONSIBILITIES;

        members.sendTo2n(new NodeMsg.ResponsabilityRequest(reqId, members.getSelfId()));
        return keepSameState();
    }

    @Override
    protected AbstractState handleResponsabilityResponse(NodeMsg.ResponsabilityResponse msg) {
        if (msg.requestId() != reqId || phase != JoinPhase.RESPONSIBILITIES)
            return ignore();

        boolean enough_responded = addResponded(msg.senderId());
        boolean enough_quorum = addData(msg.data());

        if (enough_responded && enough_quorum)
            return completeJoin();
        return keepSameState();
    }

    @Override
    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != reqId)
            return ignore();

        members.sendTo(mainActorRef, new ControlMsg.JoinAck(false));
        return new Initial(super.node);
    }

    private boolean addResponded(int senderId) {
        if (responded.contains(sender()))
            return false;

        responded.add(sender());

        int lower = members.closerLower(senderId, closestLowerResponded);
        closestLowerResponded = lower;
        int higher = members.closerHigher(senderId, closestHigherResponded);
        closestHigherResponded = higher;

        int distance = members.size();
        if (lower != higher)
            distance = members.countMembersBetweenIncluded(lower, higher);
        // Ignore myself because I'm loooking at topology before i entered
        return distance - 1 <= Config.N;
    }

    private boolean addData(Map<Integer, SendableData> dataList) {
        for (var elem : dataList.entrySet()) {
            int key = elem.getKey();
            SendableData data = elem.getValue();

            var pair = receivedData.computeIfAbsent(key, _ -> new Pair<>(data, 0));
            pair.setRight(pair.getRight() + 1);
            if (pair.getLeft().version() < data.version())
                pair.setLeft(data);
        }

        return hasSufficientReplicas();
    }

    private boolean hasSufficientReplicas() {
        for (var pair : receivedData.values()) {
            if (pair.getRight() < Config.R)
                return false;
        }
        return true;
    }

    private AbstractState completeJoin() {
        for (var entry : receivedData.entrySet())
            storage.put(entry.getKey(), entry.getValue().getLeft());

        members.sendToAll(new NotifyMsg.NodeJoined(members.getSelfId()));
        members.sendTo(mainActorRef, new ControlMsg.JoinAck(true));
        return new Normal(super.node);
    }
}
