package node;


import akka.actor.typed.javadsl.ActorContext;
import messages.Message;

public class Node {
    private final MemberManager members;
    private final DataStorage storage;
    private int lastRequestId;

    public Node(int selfId, ActorContext<Message> context) {
        this.members = new MemberManager(selfId, context);
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
