package messages.client;

import java.io.Serializable;

public abstract class DataMsg implements Serializable {
    public record Get(int requestId, int key, Integer last_version_seen) implements Serializable {}

    public record Update(int requestId, int key, String newValue, Integer last_version_seen) implements Serializable {}
}