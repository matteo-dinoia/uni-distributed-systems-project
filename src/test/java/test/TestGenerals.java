package test;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import node.NodeState;
import org.junit.ClassRule;
import org.junit.Test;
import tester.Client;
import tester.Tester;
import utils.Utils;

import java.util.Set;

public class TestGenerals {
    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void creation() {
        try (Tester tester = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            Utils.ignore(tester);
        }
    }

    @Test
    public void creationIsConsistent() {
        try (Tester tester = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            var _ = tester.getNodeStorages();
        }
    }

    @Test(expected = RuntimeException.class)
    public void invalidCreation() {
        try (Tester tester = new Tester(testKit, Set.of(1))) {
            Utils.ignore(tester);
        }
    }

    @Test
    public void crashExistent() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(5);
            assert test.getNodeState(5) == NodeState.CRASHED;
        }
    }

    @Test(expected = RuntimeException.class)
    public void crashNotExistent() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(6);
        }
    }

    @Test
    public void crashAndRecover() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(3);
            boolean recovered = test.recover(3);
            assert recovered;
        }
    }

    @Test
    public void join() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            assert test.join(6);
        }
    }

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
            var _ = test.getNodeStorages();
        }
    }

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
            storages.assertLatest(2, 0);
        }
    }

    @Test
    public void writeThenRead() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            Client client = test.getClient();
            assert test.write(client, 2, 3);
            var _ = test.getNodeStorages();
            assert test.read(client, 2, 5);
            var _ = test.getNodeStorages();
        }
    }


}
