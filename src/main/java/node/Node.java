package node;

import akka.actor.ActorContext;
import akka.actor.ActorRef;

public class Node {
    private final MemberManager members;
    private final DataStorage storage;
    private int lastRequestId;

    public Node(int selfId, ActorRef self, ActorContext context) {
        this.members = new MemberManager(selfId, self, context);
        this.storage = new DataStorage();
        this.lastRequestId = -1;
    }

    public MemberManager members() {
        return this.members;
    }

    public DataStorage storage() {
        return this.storage;
    }

    public int getFreshRequestId() {
        return ++this.lastRequestId;
    }
}
