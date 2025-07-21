package messages.node_operation;

import akka.actor.ActorRef;
import node.DataElement;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class NodeMsg {
    public record BootstrapRequest(int requestId) implements Serializable {}

    public record BootstrapResponse(int requestId, HashMap<Integer, ActorRef> updatedMembers) implements Serializable {}

    public record ResponsabilityRequest(int requestId, ActorRef requester) implements Serializable {}

    public record ResponsabilityResponse(int requestId, int senderId,
                                         HashMap<Integer, DataElement> data) implements Serializable {}

    public record Timeout(int operationId) implements Serializable {}

    // TODO Use hashmap of key, data
    public record PassResponsabilityRequest(int key, DataElement data, int requestId) implements Serializable {}

    public record PassResponsabilityResponse(List<Integer> keys, int requestId) implements Serializable {}

    public record RollbackPassResponsability(int requestId) implements Serializable {}

    // Request a read‑lock on a key
    public record LockRequest(int requestId, int key) implements Serializable {}

    // Replica grants the lock (and returns its current DataElement)
    // TODO use version instead of full element
    public record LockGranted(int requestId, DataElement element) implements Serializable {}

    // Replica denies the lock (busy or other write in progress)
    public record LockDenied(int requestId) implements Serializable {}

    // After commit/abort, release the lock
    public record LockRelease(int requestId, int key) implements Serializable {}

}
