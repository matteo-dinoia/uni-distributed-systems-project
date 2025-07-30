package messages.client;

import java.io.Serializable;

public abstract class ResponseMsgs {
    // READ
    public record ReadSucceeded(int key, String value, int version) implements Serializable {
    }

    public record ReadResultFailed(int key) implements Serializable {
    }

    public record ReadResultInexistentValue(int key) implements Serializable {
    }

    public record ReadTimeout(int key) implements Serializable {
    }

    // WRITE

    public record WriteSucceeded(int key, String newValue, int newVersion) implements Serializable {
    }

    public record WriteTimeout(int key) implements Serializable {
    }
}
