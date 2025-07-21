package states.sub;

import akka.actor.ActorRef;
import messages.client.DataMsg;
import messages.client.ResponseMsgs;
import messages.node_operation.NodeDataMsg;
import messages.node_operation.NodeMsg;
import node.Node;
import node.NodeState;
import states.AbstractState;
import states.Normal;
import utils.Config;

import java.util.HashSet;

public class Update extends AbstractState {
    private final int requestId;
    private final int key;
    private final String newValue;
    private final ActorRef client;
    private boolean sentOkToClient = false;

    // Phase 1: track which replicas granted or denied locks
    private final HashSet<ActorRef> lockGranted = new HashSet<>();
    private final HashSet<ActorRef> lockDenied = new HashSet<>();
    private Integer lastVersionSeen = null;

    // Phase 2: track write acknowledgments
    private final HashSet<ActorRef> writeAcked = new HashSet<>();

    public Update(Node node, ActorRef client, DataMsg.Update msg) {
        super(node);
        this.requestId = node.getFreshRequestId();
        this.client = client;
        this.key = msg.key();
        this.newValue = msg.newValue();
        initiateLockPhase();
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.OTHER;
    }

    /**
     * Phase 1: Send a LockRequest to all replicas responsible for this key,
     * then schedule a timeout for the entire update operation.
     */
    private void initiateLockPhase() {
        members.sendToDataResponsible(key, new NodeMsg.LockRequest(requestId, key));
        members.scheduleSendTimeoutToMyself(requestId);
    }

    /**
     * Handle a granted lock response. Record the replica's DataElement,
     * and once have W locks, compute the new version and go ahed
     */
    @Override
    protected AbstractState handleLockGranted(NodeMsg.LockGranted msg) {
        if (msg.requestId() != requestId) return ignore();
        if (sentOkToClient) {
            members.sendTo(sender(), new NodeMsg.LockRelease(requestId, key));
        }

        // TODO MAYBE just ignore it
        boolean wasAlreadyPresent = !lockGranted.add(sender());
        if (!wasAlreadyPresent)
            return panic(); // should not happen (it is a form of assertion)

        int version = msg.element().getVersion();
        if (lastVersionSeen == null || lastVersionSeen < version)
            lastVersionSeen = version;

        // When at least W locks, move to the write phase
        if (lockGranted.size() >= Config.W) {
            int newVersion = lastVersionSeen + 1;

            // reply to client as soon as W locks secured
            // From now on timeout will be ignored (as operation need to terminate now)
            members.sendTo(client, new ResponseMsgs.WriteResult(key, newValue, newVersion));
            this.sentOkToClient = true;

            // Phase 2: send WriteRequest to exactly those replicas that granted the lock
            NodeDataMsg.WriteRequest writeReq = new NodeDataMsg.WriteRequest(requestId, key, newValue, newVersion);
            members.sendTo(lockGranted.stream(), writeReq);
        }

        return keepSameState();
    }

    /**
     * Handle a denied lock response. If too many replicas deny, abort the update.
     */
    @Override
    protected AbstractState handleLockDenied(NodeMsg.LockDenied msg) {
        if (sentOkToClient || msg.requestId() != requestId) return ignore();

        lockDenied.add(sender());
        int totalReplicas = members.getMemberList().size();

        // If denials exceed N - W, we can never get W locks then abort
        if (lockDenied.size() > totalReplicas - Config.W)
            return abortOperation();

        return keepSameState();
    }

    /**
     * Handle write acknowledgments from replicas. Once all
     * chosen replicas ack, release locks and return to Normal.
     */
    @Override
    protected AbstractState handleWriteAck(NodeDataMsg.WriteAck msg) {
        if (!sentOkToClient || msg.requestId() != requestId) return ignore();

        writeAcked.add(sender());
        if (writeAcked.size() >= lockGranted.size()) {
            members.sendTo(lockGranted.stream(), new NodeMsg.LockRelease(requestId, key));
            return new Normal(super.node);
        }

        return keepSameState();
    }

    @Override
    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != requestId) return ignore();

        return abortOperation();
    }

    private AbstractState abortOperation() {
        members.sendTo(client, new ResponseMsgs.ReadTimeout(key));
        members.sendTo(lockGranted.stream(), new NodeMsg.LockRelease(requestId, key));
        return new Normal(super.node);
    }
}
