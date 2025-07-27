package messages.node_operation;

import akka.actor.ActorRef;
import node.DataElement;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class NodeMsg {
    // BOOTSTRAP

    public record BootstrapRequest(int requestId) implements Serializable {}

    public record BootstrapResponse(int requestId, HashMap<Integer, ActorRef> updatedMembers) implements Serializable {}

    // RESPONSABILITY

    public record ResponsabilityRequest(int requestId, ActorRef requester) implements Serializable {}

    public record ResponsabilityResponse(int requestId, int senderId,
                                         HashMap<Integer, DataElement> data) implements Serializable {}

    // TODO Use hashmap of key, data
    public record PassResponsabilityRequest(int key, DataElement data, int requestId) implements Serializable {}

    public record PassResponsabilityResponse(List<Integer> keys, int requestId) implements Serializable {}

    public record RollbackPassResponsability(int requestId) implements Serializable {}

    // TIMEOUT

    public record Timeout(int operationId) implements Serializable {}
}
