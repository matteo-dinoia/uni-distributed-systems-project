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

    // TODO TEST
    @Test
    public void multipleWriteOnSame() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            var res = test.clientOperations(Map.ofEntries(
                    write(test.getClient(), 1, 2),
                    write(test.getClient(), 1, 3),
                    write(test.getClient(), 1, 4)
            ));

            // Still valid
            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(1, res.size() - 1);
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
}
