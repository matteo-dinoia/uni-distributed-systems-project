package messages.client;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.HashMap;

public abstract class StatusMsg implements Serializable {
    public final int requestId;

    protected StatusMsg(int requestId) {
        this.requestId = requestId;
    }

    public static class Join extends StatusMsg {
        public final ActorRef bootstrappingPear;

        public Join(int requestId, ActorRef bootstrappingPear) {
            super(requestId);
            this.bootstrappingPear = bootstrappingPear;
        }
    }

    public class Leave extends StatusMsg {
        public Leave(int requestId) {
            super(requestId);
        }
    }

    public class Crash extends StatusMsg {
        public Crash(int requestId) {
            super(requestId);
        }
    }

    public class Recover extends StatusMsg {
        public final ActorRef bootstrappingPear;

        public Recover(int requestId, ActorRef bootstrappingPear) {
            super(requestId);
            this.bootstrappingPear = bootstrappingPear;
        }
    }

    public static class InitialMembers extends StatusMsg {
        public final HashMap<Integer, ActorRef> initial;

        public InitialMembers(int requestId, HashMap<Integer, ActorRef> initial) {
            super(requestId);
            this.initial = initial;
        }
    }
}


