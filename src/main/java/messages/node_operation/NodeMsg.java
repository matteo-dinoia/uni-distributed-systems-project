package messages.node_operation;

import akka.actor.typed.ActorRef;
import messages.Message;
import node.DataElement;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class NodeMsg {
    // BOOTSTRAP

    public record BootstrapRequest(int requestId) implements Serializable {
    }

    public record BootstrapResponse(int requestId, HashMap<Integer, ActorRef<Message>> updatedMembers) implements Serializable {
    }

    // RESPONSABILITY

    public record ResponsabilityRequest(int requestId, ActorRef<Message> requester) implements Serializable {
    }

    public record ResponsabilityResponse(int requestId, int senderId,
                                         HashMap<Integer, DataElement> data) implements Serializable {
    }

    // TODO Use hashmap of key, data
    public record PassResponsabilityRequest(int requestId, HashMap<Integer, DataElement> responsabilities) implements Serializable {
    }

    // TODO FIX never used
    public record PassResponsabilityResponse(List<Integer> keys, int requestId) implements Serializable {
    }

    public record RollbackPassResponsability(int requestId) implements Serializable {
    }

    // TIMEOUT

    public record Timeout(int operationId) implements Serializable {
    }
}
