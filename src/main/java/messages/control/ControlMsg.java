package messages.control;

import java.io.Serializable;

public class ControlMsg {
    public record LeaveAck() implements Serializable {}

    public record RecoverAck() implements Serializable {}

    public record CrashAck() implements Serializable {}

    public record JoinAck() implements Serializable {}

    public record WriteFullyCompleted() implements Serializable {}
}
