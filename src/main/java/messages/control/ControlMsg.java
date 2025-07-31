package messages.control;

import node.DataElement;
import node.NodeState;

import java.io.Serializable;
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

    public record DebugCurrentStorageResponse(int nodeId, Map<Integer, DataElement> data) implements Serializable {
    }
}
