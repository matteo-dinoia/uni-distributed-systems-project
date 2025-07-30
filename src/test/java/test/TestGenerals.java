package test;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import node.NodeState;
import org.junit.ClassRule;
import org.junit.Test;
import tester.Client;
import tester.ClientOperation;
import tester.Tester;
import utils.Utils;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

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
            boolean joined = test.join(6);
            assert joined;
        }
    }

    @Test
    public void leave() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            boolean left = test.leave(4);
            assert left;
        }
    }

    @Test
    public void writeThenLeave() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            Client client = test.getClient();
            int succedeed = test.clientOperation(Map.ofEntries(entry(client, ClientOperation.newWrite(2, 3))));
            assert succedeed == 1;

            boolean left = test.leave(3);
            assert left;
        }
    }

    @Test
    public void readInexistentValid() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 4, 5, 6))) {
            Client client = test.getClient();
            int succedeed = test.clientOperation(Map.ofEntries(entry(client, ClientOperation.newRead(3, 5))));
            assert succedeed == 1;
        }
    }

    @Test
    public void writeNew() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            Client client = test.getClient();
            int succedeed = test.clientOperation(Map.ofEntries(entry(client, ClientOperation.newWrite(2, 3))));
            assert succedeed == 1;
            var _ = test.getNodeStorages();

        }
    }

    @Test
    public void writeThenRead() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            Client client = test.getClient();
            int succedeed = test.clientOperation(Map.ofEntries(entry(client, ClientOperation.newWrite(2, 3))));
            assert succedeed == 1;
            var _ = test.getNodeStorages();
            succedeed = test.clientOperation(Map.ofEntries(entry(client, ClientOperation.newRead(2, 5))));
            assert succedeed == 1;
            var _ = test.getNodeStorages();
        }
    }

    @Test
    public void writeNew2() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            Client client = test.getClient();
            int succedeed = test.clientOperation(Map.ofEntries(entry(client, ClientOperation.newWrite(2, 3))));
            assert succedeed == 1;

            // Check actually written
            var storages = test.getNodeStorages();
            storages.assertLatest(2, 0);
        }
    }


}
