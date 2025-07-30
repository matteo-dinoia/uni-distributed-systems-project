package tester;

import java.util.Random;

public class ClientOperation {
    public enum OperationType {READ, WRITE}

    private final String value;
    private final OperationType operation;
    private final int key;
    private final int nodeId;

    public ClientOperation(OperationType operation, int key, int nodeId) {
        this.operation = operation;
        this.key = key;
        this.nodeId = nodeId;
        this.value = operation == OperationType.READ ? null : "randValue=" + new Random().nextInt();
    }

    public static ClientOperation newRead(int key, int nodeId) {
        return new ClientOperation(OperationType.READ, key, nodeId);
    }

    public static ClientOperation newWrite(int key, int nodeId) {
        return new ClientOperation(OperationType.WRITE, key, nodeId);
    }

    public boolean isRead() {
        return operation == OperationType.READ;
    }

    public String newValue() {
        assert !isRead() : "Trying to get newValue of read in ClientOperation";
        return value;
    }

    public int key() {
        return key;
    }

    public int nodeId() {
        return nodeId;
    }
}
