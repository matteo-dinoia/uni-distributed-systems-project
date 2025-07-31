package messages.node_operation;

import akka.actor.typed.ActorRef;
import messages.Message;
import node.DataElement;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NodeMsg {
    // BOOTSTRAP

    public record BootstrapRequest(int requestId) implements Serializable {
    }

    public record BootstrapResponse(int requestId, Map<Integer, ActorRef<Message>> updatedMembers) implements Serializable {
        public BootstrapResponse(int requestId, Map<Integer, ActorRef<Message>> updatedMembers) {
            this.requestId = requestId;
            this.updatedMembers = Collections.unmodifiableMap(updatedMembers);
        }
    }

    // RESPONSABILITY

    public record ResponsabilityRequest(int requestId, int newNodeId) implements Serializable {
    }

    public record ResponsabilityResponse(int requestId, int senderId, Map<Integer, DataElement> data) implements Serializable {
        public ResponsabilityResponse(int requestId, int senderId, Map<Integer, DataElement> data) {
            this.requestId = requestId;
            this.senderId = senderId;

            var deepCopy = new HashMap<>(data);
            deepCopy.replaceAll((_, v) -> v.clone());
            this.data = deepCopy;
        }
    }

    public record PassResponsabilityRequest(int requestId, Map<Integer, DataElement> responsabilities) implements Serializable {
        public PassResponsabilityRequest(int requestId, Map<Integer, DataElement> responsabilities) {
            this.requestId = requestId;

            var deepCopy = new HashMap<>(responsabilities);
            deepCopy.replaceAll((_, v) -> v.clone());
            this.responsabilities = deepCopy;
        }
    }

    public record PassResponsabilityResponse(int requestId, Set<Integer> keys) implements Serializable {
        public PassResponsabilityResponse(int requestId, Set<Integer> keys) {
            this.requestId = requestId;
            this.keys = Collections.unmodifiableSet(keys);
        }
    }

    public record RollbackPassResponsability(int requestId) implements Serializable {
    }

    // TIMEOUT

    public record Timeout(int operationId) implements Serializable {
    }
}
