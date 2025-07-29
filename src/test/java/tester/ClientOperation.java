package tester;

public interface ClientOperation {
    record Read(int key, int nodeId) implements ClientOperation {
    }

    record Write(int key, int nodeId) implements ClientOperation {
    }
}
