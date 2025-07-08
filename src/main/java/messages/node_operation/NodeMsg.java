package messages.node_operation;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.HashMap;

public class NodeMsg implements Serializable {
    public final int requestId;

    protected NodeMsg(int requestId) {
        this.requestId = requestId;
    }

    public static class BootstrapRequest extends NodeMsg {

        public BootstrapRequest(int requestId) {
            super(requestId);
        }
    }

    public static class BootstrapResponse extends NodeMsg {
        public final HashMap<Integer, ActorRef> updatedMembers;

        public BootstrapResponse(int requestId, HashMap<Integer, ActorRef> updatedMembers) {
            super(requestId);
            this.updatedMembers = updatedMembers;
        }
    }

    public static class ChangeResponsabilityRequest extends NodeMsg {

    }

    public static class ChangeResponsabilityResponse extends NodeMsg {

    }
}
