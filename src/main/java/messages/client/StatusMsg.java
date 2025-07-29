package messages.client;

import akka.actor.typed.ActorRef;
import messages.Message;

import java.io.Serializable;
import java.util.Map;

public abstract class StatusMsg {
    public record Leave() implements Serializable {
    }

    public record Crash() implements Serializable {
    }

    public record Join(ActorRef<Message> bootstrappingPear) implements Serializable {
    }

    public record Recover(ActorRef<Message> bootstrappingPear) implements Serializable {
    }

    // Skip from initial state to alive state (ignoring joining as all are joining at same time)
    public record InitialMembers(Map<Integer, ActorRef<Message>> initial) implements Serializable {
    }
}
