package messages.control;

import akka.actor.typed.ActorRef;
import messages.Message;

import java.io.Serializable;

public class ControlMsg {

    public static record NewMemberJoined(int nodeId, ActorRef<Message> replyTo) implements Serializable {
    }

    public static record NewMemberJoinedAck() implements Serializable {
    }

}
