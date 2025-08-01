package messages.node_operation;

import akka.actor.typed.ActorRef;
import messages.Message;
import node.SendableData;

import java.io.Serializable;
import java.util.Collections;
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

    public record ResponsabilityResponse(int requestId, int senderId, Map<Integer, SendableData> data) implements Serializable {
        public ResponsabilityResponse(int requestId, int senderId, Map<Integer, SendableData> data) {
            this.requestId = requestId;
            this.senderId = senderId;
            this.data = Collections.unmodifiableMap(data);
        }
    }

    public record PassResponsabilityRequest(int requestId, Map<Integer, SendableData> responsabilities) implements Serializable {
        public PassResponsabilityRequest(int requestId, Map<Integer, SendableData> responsabilities) {
            this.requestId = requestId;
            this.responsabilities = Collections.unmodifiableMap(responsabilities);
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
