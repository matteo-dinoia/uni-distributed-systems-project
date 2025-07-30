package tester;

public record ClientOperation(OperationType operation, int key, int nodeId) {
    public enum OperationType {READ, WRITE}

    public static ClientOperation newRead(int key, int nodeId) {
        return new ClientOperation(OperationType.READ, key, nodeId);
    }

    public static ClientOperation newWrite(int key, int nodeId) {
        return new ClientOperation(OperationType.WRITE, key, nodeId);
    }

    public boolean isRead() {
        return operation == OperationType.READ;
    }
}
