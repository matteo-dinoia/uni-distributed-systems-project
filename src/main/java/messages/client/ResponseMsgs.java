package messages.client;

import java.io.Serializable;

public abstract class ResponseMsgs {
    public record Completed() implements Serializable {}

    public record Timeouted() implements Serializable {}

    public record Invalid() implements Serializable {}
}
