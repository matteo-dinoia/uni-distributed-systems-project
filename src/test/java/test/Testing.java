package test;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import node.NodeState;
import org.junit.ClassRule;
import org.junit.Test;
import tester.Tester;
import utils.Utils;

import java.util.Set;

public class Testing {
    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testCreation() {
        try (Tester tester = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            Utils.ignore(tester);
        }
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidCreation() {
        try (Tester tester = new Tester(testKit, Set.of(1))) {
            Utils.ignore(tester);
        }
    }

    @Test
    public void testCrashExistent() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(5);
            assert test.getNodeState(5) == NodeState.CRASHED;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testCrashNotExistent() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(6);
        }
    }

    @Test
    public void testCrashAndRecover() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(3);
            boolean recovered = test.recover(3);
            assert recovered;
        }
    }
}
