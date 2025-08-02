package actor.node;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import messages.Message;

public class NodeInfo {
    private final int selfId;
    private final ActorContext<Message> context;

    public NodeInfo(int selfId, ActorContext<Message> context) {
        this.selfId = selfId;
        this.context = context;
    }

    public int id() {
        return selfId;
    }

    public ActorContext<Message> context() {
        return context;
    }

    public ActorRef<Message> self() {
        return context.getSelf();
    }
}
