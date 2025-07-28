package messages.node_operation;

import akka.actor.typed.ActorRef;
import messages.Message;

import java.io.Serializable;

public class NotifyMsg {
    public record NodeJoined(int actorId, ActorRef<Message> actorRef) implements Serializable {
    }

    public record NodeLeft(int actorId, ActorRef<Message> actorRef) implements Serializable {
    }
}
