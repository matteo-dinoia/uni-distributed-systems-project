package tester;

import actor.node.storage.SendableData;
import akka.actor.testkit.typed.javadsl.TestProbe;
import messages.Message;

import java.util.HashMap;

public class Client {
    /// Latest version seen of the given key element
    private final HashMap<Integer, SendableData> latestVersionSeen;
    private final TestProbe<Message> receiver;

    public Client(TestProbe<Message> receiver) {
        this.receiver = receiver;
        this.latestVersionSeen = new HashMap<>();
    }

    public TestProbe<Message> getReceiver() {
        return receiver;
    }

    public void setKeyLatestVersion(int key, SendableData newElem) {
        SendableData old = latestVersionSeen.computeIfAbsent(key, _ -> newElem);
        assert newElem.version() >= old.version() : "Client consistency is broken";

        assert newElem.version() != old.version() || newElem.value().equals(old.value()) : "Two same version with different value";

        latestVersionSeen.put(key, newElem);
    }

    public Integer latestVersionOf(int key) {
        SendableData data = latestVersionSeen.get(key);
        return data == null ? null : data.version();
    }

    public String latestValueOf(int key) {
        SendableData data = latestVersionSeen.get(key);
        return data == null ? null : data.value();
    }

    @Override
    public String toString() {
        return "Client-" + receiver.getRef().path().name();
    }
}
