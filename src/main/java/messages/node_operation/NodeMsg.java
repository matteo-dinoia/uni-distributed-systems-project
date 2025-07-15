package messages.node_operation;

import akka.actor.ActorRef;
import node.DataElement;

import java.io.Serializable;
import java.util.HashMap;

public class NodeMsg {
    public record BootstrapRequest(int requestId) implements Serializable {}

    public record BootstrapResponse(int requestId, HashMap<Integer, ActorRef> updatedMembers) implements Serializable {}

    public record ResponsabilityRequest(int requestId, ActorRef requester) implements Serializable {}

    public record ResponsabilityResponse(int requestId, int senderId,
                                         HashMap<Integer, DataElement> data) implements Serializable {}

    //    public record ChangeResponsabilityRequest() implements Serializable {}
//
//    public record ChangeResponsabilityResponse() implements Serializable {}
//
    public record Timeout(int operationId) implements Serializable {}

    public record NotifyNewNodeUp(int nodeId) implements Serializable {}
}
