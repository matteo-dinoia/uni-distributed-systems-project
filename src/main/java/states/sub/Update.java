package states.sub;

import akka.actor.typed.ActorRef;
import messages.Message;
import messages.client.DataMsg;
import messages.client.ResponseMsgs;
import messages.control.ControlMsg;
import messages.node_operation.NodeDataMsg;
import messages.node_operation.NodeMsg;
import node.Node;
import node.NodeState;
import states.AbstractState;
import states.Normal;
import utils.Config;

import java.util.HashSet;

//    a tutti gli mandi voglio scrivere, W rispondono, a quei W rispondi "io ho intenzione di scrivere",
//    bloccatevi in lettura e poi scrivo il valore, una volta scritto libero i lock in lettura e i lock in scrittura
//    e fare i controlli che siano validi. I lock sono sequenziali (prima lock lettura e poi write),
//    fare in modo che il DataElement faccia qualche tipo di controllo.
//    La read puo fallire quando chiedi a un nodo e dentro la classe get() bisogna anche gestire quel caso.

// TODO: use phase = 1 as phase indicator
// if in wrong phase either ignore or panic
// need three phases: 1 as normal
// 2 very similar to 3 but ask to lock in reading
// so no partial read (tecnically not a problem but ok)
// 3 as the current second phase

// TODO in NORMAL if is read locked when trying to read then
// add in the dataElem that a client is waiting for the new value
// send only when the readLock is released
// TODO ALTERNATIVE immediately return coudn't read and handle readFailure in
// get similarly to what we do in update

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

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.SUB;
    }


    /**
     * Phase 1: Send a LockRequest to all replicas responsible for this key,
     * then schedule a timeout for the entire update operation.
     */
    private void initiateReadLockPhase() {
        // Phase 1: request read-lock from all responsible replicas
        members.sendToDataResponsible(key, new NodeDataMsg.WriteLockRequest(requestId, key));
        members.scheduleSendTimeoutToMyself(requestId);
    }


    /**
     * Handle a granted lock response. Record the replica's DataElement,
     * and once have W locks, compute the new version and go ahed
     */
    @Override
    protected AbstractState handleWriteLockGranted(NodeDataMsg.WriteLockGranted msg) {
        if (msg.requestId() != requestId) return ignore();

        if (phase != Phase.WRITE_LOCK) {
            members.sendTo(sender(), new NodeDataMsg.WriteLockRelease(requestId, key));
            return keepSameState();
        }

        writeLockGranted.add(sender());
        // update the highest version seen so far
        if (lastVersionSeen == null || lastVersionSeen < msg.version())
            lastVersionSeen = msg.version();

        if (writeLockGranted.size() >= Config.W) {
            phase = Phase.READ_LOCK;
            newVer = lastVersionSeen + 1;

            // respond to the client with the write result
            members.sendTo(client, new ResponseMsgs.WriteSucceeded(key, newValue, newVer));

            // send the write request only to replicas that granted the lock
            var writeReq = new NodeDataMsg.ReadLockRequest(requestId, key);
            for (ActorRef<Message> ref : this.writeLockGranted)
                members.sendTo(ref, writeReq);
        }

        return keepSameState();
    }

    /**
     * Handle a denied lock response. If too many replicas deny, abort the update.
     */
    @Override
    protected AbstractState handleWriteLockDenied(NodeDataMsg.WriteLockDenied msg) {
        if (msg.requestId() != requestId) return ignore();
        if (phase != Phase.WRITE_LOCK) return ignore();

        writeLockDenied.add(sender());

        // If denials exceed N - W, we can never get W locks then abort
        if (Config.N - writeLockDenied.size() < Config.W)
            return abortOperation();
        return keepSameState();
    }

    @Override
    protected AbstractState handleReadLockAcked(NodeDataMsg.ReadLockAcked msg) {
        if (phase != Phase.READ_LOCK || msg.requestId() != requestId) return ignore();

        readLockAcked.add(sender());
        if (readLockAcked.size() >= writeLockGranted.size()) {
            phase = Phase.WRITE_AND_RELEASE;
            if (newVer == null) return panic();
            members.sendTo(writeLockGranted.stream(), new NodeDataMsg.WriteRequest(requestId, key, newValue, newVer));
        }
        return keepSameState();
    }

    /**
     * Handle write acknowledgments from replicas. Once all
     * chosen replicas ack, release locks and return to Normal.
     */
    @Override
    protected AbstractState handleWriteAck(NodeDataMsg.WriteAck msg) {
        if (phase != Phase.WRITE_AND_RELEASE || msg.requestId() != requestId) return ignore();

        writeAcked.add(sender());
        if (writeAcked.size() >= writeLockGranted.size()) {
            // That is only needed for the tester
            members.sendTo(client, new ControlMsg.WriteFullyCompleted());
            return new Normal(super.node);
        }

        return keepSameState();
    }

    @Override
    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != requestId) return ignore();
        // abort if only everything has been released
        if (phase == Phase.WRITE_LOCK)
            return abortOperation();
        return keepSameState();
    }

    private AbstractState abortOperation() {
        members.sendTo(client, new ResponseMsgs.WriteTimeout(key));
        members.sendTo(writeLockGranted.stream(), new NodeDataMsg.WriteLockRelease(requestId, key));
        return new Normal(super.node);
    }
}
