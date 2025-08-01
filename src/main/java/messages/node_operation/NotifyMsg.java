package messages.node_operation;

import java.io.Serializable;

public class NotifyMsg {
    public record NodeJoined(int actorId) implements Serializable {
    }

    public record NodeLeft(int actorId) implements Serializable {
    }
}
