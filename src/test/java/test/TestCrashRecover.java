package test;

import actor.NodeState;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.junit.ClassRule;
import org.junit.Test;
import tester.Tester;

import java.util.Set;

public class TestCrashRecover {
    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

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
            assert test.recover(3);
        }
    }

    @Test
    public void crashAndRecoverFirst() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(1);
            assert test.recover(1);
        }
    }

    @Test(expected = AssertionError.class)
    public void crashCrash() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(3);
            test.crash(3);
        }
    }

    @Test
    public void recoverBootstrappingPeerCrashed() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(3);
            test.crash(2);
            assert !test.recover(3);
        }
    }

    @Test
    public void crashRecoverWrite() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            assert test.write(null, 4, 2);

            test.crash(1);
            assert test.recover(1);

            // Still valid
            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(4, 0);
        }
    }

    @Test
    public void crashRecoverMultipleWrite() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            assert test.write(null, 2, 1);
            assert test.write(null, 3, 2);
            assert test.write(null, 1, 3);
            assert test.write(null, 3, 4);
            assert test.write(null, 2, 5);

            test.crash(1);
            assert test.recover(1);

            // Still valid
            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(2, 1);
            storages.assertLatest(3, 1);
            storages.assertLatest(1, 0);
        }
    }
}
