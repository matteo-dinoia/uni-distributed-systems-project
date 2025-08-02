package states;

import actor.NodeState;
import actor.node.Node;
import actor.node.storage.SendableData;
import akka.actor.typed.ActorRef;
import messages.Message;
import messages.control.ControlMsg;
import messages.node_operation.NodeMsg;
import messages.node_operation.NotifyMsg;
import utils.Config;
import utils.structs.Editable;
import utils.structs.Ring;

import java.io.Serializable;
import java.util.*;

/**
 * Implements node join in two phases:
 * 1) BOOTSTRAP: ask a peer for the current member list
 * 2) RESPONSIBILITIES: request data for keys this node will now serve
 */
public class Joining extends AbstractState {
    private enum JoinPhase {BOOTSTRAP, RESPONSIBILITIES}

    private final ActorRef<Message> mainActorRef;
    private JoinPhase phase;

    // key: (data, how many replicas confirmed it)
    private final HashMap<Integer, SendableData> receivedData = new HashMap<>();
    private final Ring<Editable<Boolean>> responded = new Ring<>();
    private final int reqId;

    /**
     * @param bootstrapPeer a live node to bootstrap membership from
     */
    public Joining(Node node, ActorRef<Message> bootstrapPeer, ActorRef<Message> mainActorRef) {
        super(node);
        this.reqId = node.getFreshRequestId();
        this.phase = JoinPhase.BOOTSTRAP;
        this.mainActorRef = mainActorRef;
        node.sendTo(bootstrapPeer, new NodeMsg.BootstrapRequest(reqId));
        node.scheduleTimeout(reqId);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.JOINING;
    }

    // HANDLERS

    @Override
    public AbstractState handle(Serializable message) {
        return switch (message) {
            case NodeMsg.BootstrapResponse msg -> handleBootstrapResponse(msg);
            case NodeMsg.Timeout msg -> handleTimeout(msg);
            case NodeMsg.ResponsabilityResponse msg -> handleResponsabilityResponse(msg);
            default -> log_unhandled(message);
        };
    }

    protected AbstractState handleBootstrapResponse(NodeMsg.BootstrapResponse msg) {
        if (msg.requestId() != reqId || phase != JoinPhase.BOOTSTRAP)
            return ignore();

        members.setMembers(msg.updatedMembers());
        phase = JoinPhase.RESPONSIBILITIES;

        List<ActorRef<Message>> toCommunicate = members.getNodeToCommunicateForJoin();
        createResponded(msg.updatedMembers(), new HashSet<>(toCommunicate));

        node.sendTo(toCommunicate.stream(), new NodeMsg.ResponsabilityRequest(reqId, node.id()));
        return keepSameState();
    }

    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != reqId)
            return ignore();

        // Failed and as such exit
        node.sendTo(mainActorRef, new ControlMsg.JoinAck(false));
        return new Left(super.node);
    }

    protected AbstractState handleResponsabilityResponse(NodeMsg.ResponsabilityResponse msg) {
        if (msg.requestId() != reqId || phase != JoinPhase.RESPONSIBILITIES)
            return ignore();

        addData(msg.senderId(), msg.data());

        if (enoughResponded())
            return completeJoin();

        return keepSameState();
    }

    // PRIVATE METHODS

    private void createResponded(Map<Integer, ActorRef<Message>> otherNodes, Set<ActorRef<Message>> toCommunicate) {
        HashMap<Integer, Editable<Boolean>> map = new HashMap<>();
        for (var entry : otherNodes.entrySet()) {
            boolean communicate = toCommunicate.contains(entry.getValue());
            map.put(entry.getKey(), new Editable<>(!communicate));
        }

        responded.replaceAll(map);
    }

    private boolean enoughResponded() {
        return responded.verifyNValidInMSizedWindows(Config.R, Config.N, x -> x.valid);
    }

    private void addData(int senderId, Map<Integer, SendableData> dataList) {
        var eb = responded.get(senderId);
        if (eb.valid)
            return;
        eb.valid = true;

        // Add new data
        for (var entry : dataList.entrySet()) {
            int key = entry.getKey();

            SendableData other = entry.getValue();
            SendableData existing = receivedData.get(key);

            if (existing == null || existing.version() < other.version())
                receivedData.put(key, other);
        }
    }

    private AbstractState completeJoin() {
        for (var entry : receivedData.entrySet())
            storage.put(entry.getKey(), entry.getValue());

        node.sendToAll(new NotifyMsg.NodeJoined(node.id()));
        node.sendTo(mainActorRef, new ControlMsg.JoinAck(true));
        return new Normal(super.node);
    }
}
