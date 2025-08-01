package test;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.junit.ClassRule;
import org.junit.Test;
import tester.Client;
import tester.Tester;

import java.util.Set;

public class TestLeave {
    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void leave() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            assert test.leave(4);
        }
    }

    @Test
    public void writeThenLeave() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            assert test.write(null, 2, 3);
            assert test.leave(3);

            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(2, 0);
        }
    }

    @Test(expected = AssertionError.class)
    public void leaveNodeCrashed() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(3);
            assert test.getNodeState(3) == node.NodeState.CRASHED;
            test.leave(3);
        }
    }

    @Test
    public void join2WriteLeave() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            int key = 1;

            assert test.join(6);
            test.getNodeStorages().assertValid();
            assert test.write(null, key, 2);
            test.getNodeStorages().assertValid();
            assert test.write(null, key, 4);
            var storages = test.getNodeStorages();
            storages.assertValid();


            assert test.leave(2);
            storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(key, 1);
        }
    }

    @Test
    public void leaveWritersCheckQuorumPreserved2() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            int key = 1;

            assert test.join(6);
            assert test.join(7);
            assert test.join(8);

            assert test.write(null, key, 2);
            assert test.write(null, key, 4);
            assert test.write(null, key, 6);

            assert test.leave(2);
            assert test.leave(4);
            assert test.leave(6);

            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(key, 2);
        }
    }

    @Test
    public void simpleWriteAndLeaveCheckQuorum() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            int key = 100;
            Client client = test.getClient();

            assert test.write(client, key, 2);

            int version = client.latestVersionOf(key);

            assert test.leave(2);

            var storages = test.getNodeStorages();

            storages.assertLatest(key, version);
        }
    }

    @Test
    public void leaveRollbackKeepsKeys() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            final int key = 6;

            test.crash(1);
            assert test.write(null, key, 4);
            test.crash(2);

            assert !test.leave(4) : "Leave should fail due to timeout by triggering rollback";

            var storages = test.getNodeStorages();
            storages.printKeyStatus(key);
            storages.assertValid();
            storages.assertLatest(key, 0);
        }
    }
}
