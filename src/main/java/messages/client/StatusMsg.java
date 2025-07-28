package messages.client;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.Map;

public abstract class StatusMsg {
    public record Leave(int requestId) implements Serializable {}

    public record Crash(int requestId) implements Serializable {}

    public record Join(int requestId, ActorRef bootstrappingPear) implements Serializable {}

    public record Recover(int requestId, ActorRef bootstrappingPear) implements Serializable {}

    // Skip from initial state to alive state (ignoring joining as all are joining at same time)
    public record InitialMembers(int requestId, Map<Integer, ActorRef> initial) implements Serializable {}
}
