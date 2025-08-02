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
            assert test.write(test.getClient(0), key, 2);
            String written = test.getClient(0).latestValueOf(key);

            var res = test.clientOperations(Map.ofEntries(
                    read(test.getClient(0), key, 2),
                    read(test.getClient(1), key, 3),
                    read(test.getClient(2), key, 4)
            ));

            assert res.size() == 3;
            assert test.getClient(0).latestVersionOf(key) == 0;
            assert test.getClient(1).latestVersionOf(key) == 0;
            assert test.getClient(2).latestVersionOf(key) == 0;

            assert test.getClient(0).latestValueOf(key).equals(written);
            assert test.getClient(1).latestValueOf(key).equals(written);
            assert test.getClient(2).latestValueOf(key).equals(written);
        }
    }

    @Test
    public void multipleReadWrite() {
        final int keyA = 2;
        final int keyB = 3;
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {

            var succ = test.clientOperations(Map.ofEntries(
                    read(test.getClient(0), keyA, 2),
                    write(test.getClient(3), keyA, 3),
                    write(test.getClient(4), keyB, 5),
                    read(test.getClient(1), keyA, 3),
                    read(test.getClient(2), keyA, 1),
                    read(test.getClient(7), keyB, 5),

                    write(test.getClient(5), keyB, 4),
                    write(test.getClient(6), keyB, 5),
                    read(test.getClient(8), keyB, 2)
            ));

            // Read must succeed (possibly with yet no value)
            assert succ.containsKey(test.getClient(0))
                    && succ.containsKey(test.getClient(1))
                    && succ.containsKey(test.getClient(2))
                    && succ.containsKey(test.getClient(7))
                    && succ.containsKey(test.getClient(8));

            // The one writing alone on keyA must succeed (unless test is too slow)
            assert succ.containsKey(test.getClient(3));

            // Still consistent
            test.getNodeStorages().assertValid();
        }
    }
}
