package tester;

import akka.actor.testkit.typed.javadsl.TestProbe;
import messages.Message;

import java.util.HashMap;

public class Client {
    /// Latest version seen of the given key element
    private final HashMap<Integer, Integer> latestVersionSeen;
    private final TestProbe<Message> receiver;

    public Client(TestProbe<Message> receiver) {
        this.receiver = receiver;
        this.latestVersionSeen = new HashMap<>();
    }

    public TestProbe<Message> getReceiver() {
        return receiver;
    }

    public void setKeyLatestVersion(int key, int newVersion) {
        Integer old = latestVersionSeen.computeIfAbsent(key, _ -> newVersion);
        assert newVersion >= old : "Client consistency is broken";

        latestVersionSeen.put(key, newVersion);
    }

    public Integer getKeyLatestVersion(int key) {
        return latestVersionSeen.get(key);
    }
}
