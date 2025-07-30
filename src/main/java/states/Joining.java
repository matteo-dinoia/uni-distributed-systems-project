package states;

import akka.actor.typed.ActorRef;
import messages.Message;
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

/**
 * Implements node join in two phases:
 * 1) BOOTSTRAP: ask a peer for the current member list
 * 2) RESPONSIBILITIES: request data for keys this node will now serve
 */
public class Joining extends AbstractState {
    private enum JoinPhase {BOOTSTRAP, RESPONSIBILITIES}

    private JoinPhase phase;

    // key: (data, how many replicas confirmed it)
    private final HashMap<Integer, Pair<DataElement, Integer>> receivedData = new HashMap<>();
    private final Set<ActorRef<Message>> responded = new HashSet<>();
    private final int reqId;

    private Integer closestHigherResponded = null;
    private Integer closestLowerResponded = null;

    /**
     * @param bootstrapPeer a live node to bootstrap membership from
     */
    public Joining(Node node, ActorRef<Message> bootstrapPeer) {
        super(node);
        this.reqId = node.getFreshRequestId();
        this.phase = JoinPhase.BOOTSTRAP;
        System.out.println("riga 43");
        members.sendTo(bootstrapPeer, new NodeMsg.BootstrapRequest(reqId));
        System.out.println("riga 45");
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

        System.out.println("BOOTSTRAP RESPONSE in node " + members.getSelfId() + ": " + msg.updatedMembers());

        members.setMemberList(msg.updatedMembers());

        members.addMember(members.getSelfId(), members.getSelfRef());

        System.out.println("AFTER setMemberList: " + members.getMemberList().keySet());

        phase = JoinPhase.RESPONSIBILITIES;
        sendInitialMsg();
        return keepSameState();
    }

    private void sendInitialMsg() {
        members.sendTo2n(new NodeMsg.ResponsabilityRequest(reqId, members.getSelfRef()));
        members.scheduleSendTimeoutToMyself(reqId);
    }

    @Override
    protected AbstractState handleResponsabilityResponse(NodeMsg.ResponsabilityResponse msg) {
        if (msg.requestId() != reqId || phase != JoinPhase.RESPONSIBILITIES)
            return ignore();

        boolean enough_responded = addResponded(msg.senderId());
        boolean enough_quorum = addData(msg.data());

        if (enough_responded && enough_quorum) {

            for (var entry : receivedData.entrySet()) {
                storage.put(entry.getKey(), entry.getValue().getLeft());
            }

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

    // TODO CHECK order of element that is counted correctly
    private boolean addResponded(int senderId) {
        if (responded.contains(sender()))
            return false;

        responded.add(sender());

        closestLowerResponded = members.closerLower(senderId, closestLowerResponded);
        closestHigherResponded = members.closerHigher(senderId, closestHigherResponded);
        return members.countMembersBetweenIncluded(closestLowerResponded, closestHigherResponded) <= Config.N;
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
