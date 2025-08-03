package messages.client;

import java.io.Serializable;

public abstract class DataMsg implements Serializable {
    public record Get(int key, Integer lastVersionSeen) implements Serializable {
    }

    public record Update(int key, Integer lastVersionSeen, String newValue) implements Serializable {
    }
}