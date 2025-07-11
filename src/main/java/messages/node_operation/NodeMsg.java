package messages.node_operation;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.HashMap;

public class NodeMsg {
    public record BootstrapRequest(int requestId) implements Serializable {}

    public record BootstrapResponse(int requestId, HashMap<Integer, ActorRef> updatedMembers) implements Serializable {}
//
//    public record ChangeResponsabilityRequest() implements Serializable {}
//
//    public record ChangeResponsabilityResponse() implements Serializable {}
//
//    public record Timeout(int operationId) implements Serializable {}
}
