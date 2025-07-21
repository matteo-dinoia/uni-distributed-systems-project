package messages.client;

import java.io.Serializable;

public abstract class ResponseMsgs {
//    public record Completed() implements Serializable {}
//
//    public record Timeouted() implements Serializable {}
//
//    public record Invalid() implements Serializable {}

    public record ReadResult(int key, String value, int version) implements Serializable {}

    public record ReadResultFailed(int key) implements Serializable {}

    public record ReadTimeout(int key) implements Serializable {}

    public record WriteResult(int key, String newValue, int newVersion) implements Serializable {}
}
