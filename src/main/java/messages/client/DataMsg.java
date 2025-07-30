package messages.client;

import java.io.Serializable;

public abstract class DataMsg implements Serializable {
    public record Get(int key, Integer last_version_seen) implements Serializable {
    }

    public record Update(int key, String newValue) implements Serializable {
    }
}