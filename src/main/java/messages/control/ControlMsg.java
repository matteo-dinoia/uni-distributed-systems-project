package messages.control;

import node.NodeState;

import java.io.Serializable;

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

    public record DebugGetCurrentState() implements Serializable {
    }

    public record DebugCurrentState(NodeState state) implements Serializable {
    }
}
