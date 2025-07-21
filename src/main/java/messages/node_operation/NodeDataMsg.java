package messages.node_operation;

import node.DataElement;

import java.io.Serializable;

public class NodeDataMsg {
    public record ReadRequest(int requestId, int key) implements Serializable {}

    public record ReadReply(int requestId, DataElement element) implements Serializable {}

    public record WriteRequest(int requestId, int key, String value, int version) implements Serializable {}

    public record WriteAck(int requestId) implements Serializable {}
}
