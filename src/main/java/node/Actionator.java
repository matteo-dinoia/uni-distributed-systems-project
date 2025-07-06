package node;


import akka.actor.ActorRef;
import messages.client.DataMsg;

@SuppressWarnings("unused")
public class Actionator {
    public final Multicaster multicaster;
    public int requestId;

    public Actionator(int requestId, Multicaster multicaster) {
        this.multicaster = multicaster;
        this.requestId = requestId;
    }

    public void handleGet(ActorRef sender, DataMsg.GetMsg msg) {
    }

    public void handleUpdate(ActorRef sender, DataMsg.UpdateMsg msg) {
    }
}
