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
import utils.Ring;

import java.util.*;

/**
 * Implements node join in two phases:
 * 1) BOOTSTRAP: ask a peer for the current member list
 * 2) RESPONSIBILITIES: request data for keys this node will now serve
 */
public class Joining extends AbstractState {
    static class EditableInteger {
        public int n;

        public EditableInteger(int n) {
            this.n = n;
        }
    }

    private final static int IGNORABLE = -1;

    private enum JoinPhase {BOOTSTRAP, RESPONSIBILITIES}

    private final ActorRef<Message> mainActorRef;
    private JoinPhase phase;

    // key: (data, how many replicas confirmed it)
    private final HashMap<Integer, SendableData> receivedData = new HashMap<>();
    private final Set<ActorRef<Message>> responded = new HashSet<>();
    private final Ring<EditableInteger> agreement = new Ring<>();
    private final int reqId;

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

        List<ActorRef<Message>> toCommunicate = members.getNodeToCommunicateForJoin();
        createAgreement(new HashSet<>(toCommunicate));

        members.sendTo(toCommunicate.stream(), new NodeMsg.ResponsabilityRequest(reqId, members.getSelfId()));
        return keepSameState();
    }

    private void createAgreement(Set<ActorRef<Message>> toCommunicate) {
        for (var entry : members.getMemberList().entrySet()) {
            int nodeId = entry.getKey();
            ActorRef<Message> nodeRef = entry.getValue();

            int value = toCommunicate.contains(nodeRef) ? 0 : IGNORABLE;
            agreement.put(nodeId, new EditableInteger(value));
        }

        // To have topology before the enter of the new node
        agreement.remove(members.getSelfId());
    }

    @Override
    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != reqId)
            return ignore();

        members.sendTo(mainActorRef, new ControlMsg.JoinAck(false));
        return new Initial(super.node);
    }

    @Override
    protected AbstractState handleResponsabilityResponse(NodeMsg.ResponsabilityResponse msg) {
        if (msg.requestId() != reqId || phase != JoinPhase.RESPONSIBILITIES)
            return ignore();

        addData(msg.senderId(), msg.data());

        if (enoughResponded())
            return completeJoin();
        return keepSameState();
    }

    private boolean enoughResponded() {
        return agreement.getHashMap().values().stream().allMatch(ei -> ei.n == IGNORABLE || ei.n >= Config.R);
    }

    private void addData(int senderId, Map<Integer, SendableData> dataList) {
        if (responded.contains(sender()))
            return;
        responded.add(sender());

        // Add new data
        for (var entry : dataList.entrySet()) {
            int key = entry.getKey();

            SendableData other = entry.getValue();
            SendableData existing = receivedData.get(key);

            if (existing == null || existing.version() < other.version())
                receivedData.put(key, other);
        }

        // Update agreement counter
        var toIncrease = agreement.getInterval(senderId, Config.N - 1, Config.N - 1);
        for (EditableInteger ei : toIncrease) {
            if (ei.n != IGNORABLE)
                ei.n++;
        }

    }

    private AbstractState completeJoin() {
        for (var entry : receivedData.entrySet())
            storage.put(entry.getKey(), entry.getValue());

        members.sendToAll(new NotifyMsg.NodeJoined(members.getSelfId()));
        members.sendTo(mainActorRef, new ControlMsg.JoinAck(true));
        return new Normal(super.node);
    }
}
