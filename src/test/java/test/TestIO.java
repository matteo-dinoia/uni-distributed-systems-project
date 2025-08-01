package test;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.junit.ClassRule;
import org.junit.Test;
import tester.Client;
import tester.Tester;

import java.util.Set;

public class TestIO {
    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void readInexistentValid() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 4, 5, 6))) {
            assert test.read(null, 3, 5);
        }
    }

    @Test
    public void writeNew() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            assert test.write(null, 2, 3);

            // Check actually written
            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(2, 0);
        }
    }

    @Test
    public void writeThenRead() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            Client client = test.getClient();
            assert test.write(client, 2, 3);
            test.getNodeStorages().assertValid();
            assert test.read(client, 2, 5);
            test.getNodeStorages().assertValid();
        }
    }

    @Test
    public void multipleWrite() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            assert test.write(null, 2, 1);
            assert test.write(null, 3, 2);
            assert test.write(null, 1, 3);
            assert test.write(null, 3, 4);
            assert test.write(null, 2, 5);

            // Still valid
            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(2, 1);
            storages.assertLatest(3, 1);
            storages.assertLatest(1, 0);
        }
    }

    @Test
    public void write1Crash() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(2);

            assert test.write(null, 1, 3);

            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(1, 0);
        }
    }

    @Test
    public void write2Crash() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(2);
            test.crash(4);

            assert !test.write(null, 1, 3);

            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertMissing(1);
        }
    }

    @Test
    public void writeLeaveWrite() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            final int key = 1;

            // Write
            assert test.write(null, key, 5);
            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(key, 0);

            // leave join
            assert test.leave(4);
            //assert test.join(6);
            storages = test.getNodeStorages();
            storages.assertLatest(key, 0);
            storages.assertValid();

            // write
            assert test.write(null, key, 5);
            storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(key, 1);
        }
    }
}
