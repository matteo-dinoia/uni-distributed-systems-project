package states;

import akka.actor.ActorRef;
import messages.client.DataMsg;
import messages.client.StatusMsg;
import node.DataStorage;
import node.MemberManager;

import java.io.Serializable;

@SuppressWarnings("unused")
// Return new state if it exist (else return this which mean no change)
public abstract class AbstractState {
    protected final DataStorage storage;
    protected final MemberManager members;
    private ActorRef sender = null;

    protected AbstractState(DataStorage storage, MemberManager memberManager) {
        this.storage = storage;
        this.members = memberManager;
    }

    // UTILS ---------------------------------------------------------------

    protected AbstractState panic() {
        System.out.println("Something catastrofic happened");
        System.exit(1);
        return null;
    }

    protected AbstractState log_unhandled() {
        System.out.println("Something not handled explicitly happened");
        return this;
    }

    protected AbstractState ignore() {
        return this;
    }

    protected ActorRef sender() {
        return this.sender;
    }

    // DISPATCHER  ---------------------------------------------------------------------

    public AbstractState handle(ActorRef sender, Serializable message) {
        this.sender = sender;

        return switch (message) {
            case StatusMsg.Recover msg -> handleRecover(msg);
            case DataMsg.GetMsg msg -> handleGet(msg);
            default -> throw new IllegalStateException("Unexpected value: " + message);
        };
    }

    // MESSAGE HANDLERS ---------------------------------------------------------------

    protected AbstractState handleRecover(StatusMsg.Recover msg) {
        return panic();
    }

    protected AbstractState handleGet(DataMsg.GetMsg msg) {
        return log_unhandled();
    }

}




