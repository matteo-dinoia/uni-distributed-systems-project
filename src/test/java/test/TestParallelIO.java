package test;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.junit.ClassRule;
import org.junit.Test;
import tester.Client;
import tester.ClientOperation;
import tester.Tester;

import java.util.Map;
import java.util.Set;

public class TestParallelIO {
    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    private Map.Entry<Client, ClientOperation> read(Client client, int key, int nodeId) {
        ClientOperation op = ClientOperation.newRead(key, nodeId);
        return Map.entry(client, op);
    }

    private Map.Entry<Client, ClientOperation> write(Client client, int key, int nodeId) {
        ClientOperation op = ClientOperation.newWrite(key, nodeId);
        return Map.entry(client, op);
    }

    @Test
    public void multipleWriteOnSame() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            var res = test.clientOperations(Map.ofEntries(
                    write(test.getClient(), 1, 2),
                    write(test.getClient(), 1, 3),
                    write(test.getClient(), 1, 4)
            ));

            //assert !res.isEmpty();

            // Still valid
            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(1, res.size() - 1);
        }
    }

    @Test
    public void multipleWriteOnDifferent() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            var res = test.clientOperations(Map.ofEntries(
                    write(test.getClient(), 1, 2),
                    write(test.getClient(), 2, 3),
                    write(test.getClient(), 3, 4)
            ));

            assert res.size() == 3;

            // Still valid
            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(1, 0);
            storages.assertLatest(2, 0);
            storages.assertLatest(3, 0);
        }
    }

    @Test
    public void multipleReadOnNotExistent() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            var res = test.clientOperations(Map.ofEntries(
                    read(test.getClient(), 1, 2),
                    read(test.getClient(), 1, 3),
                    read(test.getClient(), 1, 4)
            ));

            assert res.size() == 3;
        }
    }

    @Test
    public void multipleReadOnExistent() {
        final int key = 2;
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            Client[] clients = {test.getClient(), test.getClient(), test.getClient()};
            assert test.write(clients[0], key, 2);
            String written = clients[0].latestValueOf(1);

            var res = test.clientOperations(Map.ofEntries(
                    read(clients[0], key, 2),
                    read(clients[1], key, 3),
                    read(clients[2], key, 4)
            ));

            assert res.size() == 3;
            assert clients[0].latestVersionOf(key) == 0;
            assert clients[1].latestVersionOf(key) == 0;
            assert clients[2].latestVersionOf(key) == 0;

            assert clients[0].latestValueOf(key).equals(written);
            assert clients[1].latestValueOf(key).equals(written);
            assert clients[2].latestValueOf(key).equals(written);
        }
    }
}
