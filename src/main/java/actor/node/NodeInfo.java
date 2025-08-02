package actor.node;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import messages.Message;
import utils.Config;

public record NodeInfo(int id, ActorContext<Message> context, Config config) {
    public ActorRef<Message> self() {
        return context.getSelf();
    }
}
