package messages.control;

import node.NodeState;
import node.SendableData;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class ControlMsg {
    public record LeaveAck(boolean left) implements Serializable {
    }

    public record RecoverAck(boolean recovered) implements Serializable {
    }

    public record CrashAck() implements Serializable {
    }

    public record JoinAck(boolean joined) implements Serializable {
    }

    public record WriteFullyCompleted() implements Serializable {
    }

    public record InitialMembersAck() implements Serializable {
    }

    public record DebugCurrentStateRequest() implements Serializable {
    }

    public record DebugCurrentStateResponse(NodeState state) implements Serializable {
    }

    public record DebugCurrentStorageRequest() implements Serializable {
    }

    public record DebugCurrentStorageResponse(int nodeId, Map<Integer, SendableData.Debug> data) implements Serializable {
        public DebugCurrentStorageResponse(int nodeId, Map<Integer, SendableData.Debug> data) {
            this.nodeId = nodeId;
            this.data = Collections.unmodifiableMap(data);
        }
    }
}
