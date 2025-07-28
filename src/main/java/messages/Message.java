package messages;

import akka.actor.typed.ActorRef;

import java.io.Serializable;

public record Message(ActorRef<Message> sender, Serializable content) {
}
