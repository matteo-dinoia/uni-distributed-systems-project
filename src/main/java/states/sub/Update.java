package states.sub;

import actor.NodeState;
import actor.node.Node;
import akka.actor.typed.ActorRef;
import messages.Message;
import messages.client.DataMsg;
import messages.client.ResponseMsgs;
import messages.control.ControlMsg;
import messages.node_operation.NodeDataMsg;
import messages.node_operation.NodeMsg;
import states.AbstractState;
import states.Left;
import utils.Config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Stream;

public class Update extends AbstractState {
    // Phase indicators
    private enum Phase {WRITE_LOCK, READ_LOCK, WRITE_AND_RELEASE}

    private final int requestId;
    private final int key;
    private final String newValue;
    private final ActorRef<Message> client;
    private Phase phase;
    // Phase 1: track which replicas granted or denied write locks
    private final HashSet<ActorRef<Message>> writeLockGranted = new HashSet<>();
    private final HashSet<ActorRef<Message>> writeLockDenied = new HashSet<>();
    private Integer lastVersionSeen = null;
    // Phase 2: track read locks acknowledgments
    private Integer newVer = null;
    private final HashSet<ActorRef<Message>> readLockAcked = new HashSet<>();
    // Phase 3: track read locks acknowledgments
    private final HashSet<ActorRef<Message>> writeAcked = new HashSet<>();

    public Update(Node node, ActorRef<Message> client, DataMsg.Update msg, int requestId) {
        super(node);
        this.requestId = requestId;
        this.client = client;
        this.key = msg.key();
        this.newValue = msg.newValue();
        this.phase = Phase.WRITE_LOCK;
        initiateReadLockPhase();
    }

    /**
     * Phase 1: Send a LockRequest to all replicas responsible for this key,
     * then schedule a timeout for the entire update operation.
     */
    private void initiateReadLockPhase() {
        // Phase 1: request read-lock from all responsible replicas
        node.sendToResponsible(key, new NodeDataMsg.WriteLockRequest(requestId, key));
        node.scheduleTimeout(requestId);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.SUB;
    }

    // HANDLERS

    @Override
    public AbstractState handle(Serializable message) {
        return switch (message) {
            case NodeDataMsg.WriteLockGranted msg -> handleWriteLockGranted(msg);
            case NodeDataMsg.WriteLockDenied msg -> handleWriteLockDenied(msg);
            case NodeDataMsg.ReadLockAcked msg -> handleReadLockAcked(msg);
            case NodeDataMsg.WriteAck msg -> handleWriteAck(msg);
            case NodeMsg.Timeout msg -> handleTimeout(msg);
            default -> log_unhandled(message);
        };
    }

    /**
     * Handle a granted lock response. Record the replica's DataElement,
     * and once have W locks, compute the new version and go ahed
     */
    protected AbstractState handleWriteLockGranted(NodeDataMsg.WriteLockGranted msg) {
        if (msg.requestId() != requestId || phase != Phase.WRITE_LOCK) return ignore();

        writeLockGranted.add(sender());
        // update the highest version seen so far
        if (lastVersionSeen == null || lastVersionSeen < msg.version())
            lastVersionSeen = msg.version();

        if (writeLockGranted.size() >= Config.W) {
            phase = Phase.READ_LOCK;
            newVer = lastVersionSeen + 1;

            // respond to the client with the write result
            node.sendTo(client, new ResponseMsgs.WriteSucceeded(key, newValue, newVer));

            // send the write request only to replicas that granted the lock
            var writeReq = new NodeDataMsg.ReadLockRequest(requestId, key);
            for (ActorRef<Message> ref : this.writeLockGranted)
                node.sendTo(ref, writeReq);
        }

        return keepSameState();
    }

    /**
     * Handle a denied lock response. If too many replicas deny, abort the update.
     */
    protected AbstractState handleWriteLockDenied(NodeDataMsg.WriteLockDenied msg) {
        if (msg.requestId() != requestId) return ignore();
        if (phase != Phase.WRITE_LOCK) return ignore();

        writeLockDenied.add(sender());

        // If denials exceed N - W, we can never get W locks then abort
        if (Config.N - writeLockDenied.size() < Config.W)
            return abortOperation();
        return keepSameState();
    }

    protected AbstractState handleReadLockAcked(NodeDataMsg.ReadLockAcked msg) {
        if (phase != Phase.READ_LOCK || msg.requestId() != requestId) return ignore();

        readLockAcked.add(sender());
        if (readLockAcked.size() >= writeLockGranted.size()) {
            phase = Phase.WRITE_AND_RELEASE;
            assert newVer != null : "New version read for write operation is null";
            node.sendTo(writeLockGranted.stream(), new NodeDataMsg.WriteRequest(requestId, key, newValue, newVer));
        }
        return keepSameState();
    }

    /**
     * Handle write acknowledgments from replicas. Once all
     * chosen replicas ack, release locks and return to Normal.
     */
    protected AbstractState handleWriteAck(NodeDataMsg.WriteAck msg) {
        if (phase != Phase.WRITE_AND_RELEASE || msg.requestId() != requestId) return ignore();

        writeAcked.add(sender());
        if (writeAcked.size() >= writeLockGranted.size())
            return concludeOperation();

        return keepSameState();
    }

    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != requestId) return ignore();
        // abort if only everything has been released
        if (phase == Phase.WRITE_LOCK)
            return abortOperation();
        return keepSameState();
    }

    // PRIVATE METHODS

    private AbstractState concludeOperation() {
        // Free all lock that didn't respond
        node.sendTo(getToFree(true), new NodeDataMsg.LocksRelease(requestId, key));

        // That is only needed for the tester
        node.sendTo(client, new ControlMsg.WriteFullyCompleted());
        return new Left(super.node);
    }

    private AbstractState abortOperation() {
        // Free all lock not denied
        node.sendTo(getToFree(false), new NodeDataMsg.LocksRelease(requestId, key));

        node.sendTo(client, new ResponseMsgs.WriteTimeout(key));
        return new Left(super.node);
    }

    private Stream<ActorRef<Message>> getToFree(boolean successfulRead) {
        var res = new ArrayList<>(members.getResponsibles(key));

        if (successfulRead)
            res.removeAll(writeLockGranted);
        res.removeAll(writeLockDenied);

        return res.stream();
    }
}
