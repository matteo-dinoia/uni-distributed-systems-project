package messages.node_operation;

import actor.node.storage.SendableData;

import java.io.Serializable;

public class NodeDataMsg {
    // READ

    public record ReadRequest(int requestId, int key) implements Serializable {
    }

    public record ReadResponse(int requestId, int key, SendableData element) implements Serializable {
    }

    public record ReadImpossibleForLock(int requestId) implements Serializable {
    }

    // WRITE

    public record WriteRequest(int requestId, int key, String value, int version) implements Serializable {
    }

    public record WriteAck(int requestId) implements Serializable {
    }

    // READ LOCKS

    public record ReadLockRequest(int requestId, int key) implements Serializable {
    }

    public record ReadLockAcked(int requestId) implements Serializable {
    }

    // WRITE LOCKS

    public record WriteLockRequest(int requestId, int key) implements Serializable {
    }

    public record WriteLockGranted(int requestId, int key, int version) implements Serializable {
    }

    public record WriteLockDenied(int requestId) implements Serializable {
    }

    public record LocksRelease(int requestId, int key) implements Serializable {
    }
}
