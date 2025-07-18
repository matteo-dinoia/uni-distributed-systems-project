package messages.node_operation;

import akka.actor.ActorRef;

import java.io.Serializable;

public class NotifyMsg {
    public record NodeJoined(int actorId, ActorRef actorRef) implements Serializable {}

    public record NodeLeft(int actorId, ActorRef actorRef) implements Serializable {}
}
