package test;

import actor.NodeState;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.junit.ClassRule;
import org.junit.Test;
import tester.StorageTester;
import tester.Tester;

import java.util.Set;

public class TestJoin {
    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void join() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            assert test.join(6);
            assert test.getNodeState(6) == NodeState.NORMAL;
        }
    }

    @Test
    public void joinJoinJoinWithWrite() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            assert test.write(null, 6, 5);
            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(6, 0);

            assert test.join(6);
            storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(6, 0);

            assert test.join(7);
            storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(6, 0);

            assert test.join(8);
            storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(6, 0);

            assert test.join(9);
            storages = test.getNodeStorages();
            storages.assertValid();

            storages.assertLatest(6, 0);

            assert test.getNodeState(6) == NodeState.NORMAL;
        }
    }

    @Test
    public void joinBootstrapDown() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(5);
            assert !test.join(6);
        }
    }

    @Test
    public void joinOneOtherDown() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(4);
            assert test.join(6);
        }
    }

    @Test
    public void joinTwoOtherDown() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(4);
            test.crash(1);
            assert test.join(6);
        }
    }

    @Test
    public void joinThreeOtherDownFail() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            test.crash(1);
            test.crash(2);
            test.crash(4);
            assert !test.join(6);
        }
    }


    @Test
    public void joinWriteOneOtherDown() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            assert test.write(null, 2, 5);

            test.crash(1);
            assert test.join(6);

            // Still valid
            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(2, 0);
        }
    }

    @Test
    public void joinMultipleWriteOneOtherDown() {
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            assert test.write(null, 2, 1);
            assert test.write(null, 3, 2);
            assert test.write(null, 1, 3);
            assert test.write(null, 3, 4);
            assert test.write(null, 2, 5);

            test.crash(1);
            assert test.join(6);

            // Still valid
            var storages = test.getNodeStorages();
            storages.assertValid(Set.of(1));
            storages.assertLatest(2, 1);
            storages.assertLatest(3, 1);
            storages.assertLatest(1, 0);
        }
    }

    @Test
    public void joinMultipleWriteOneOtherDownRecover() {
        StorageTester storages;
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5))) {
            assert test.write(null, 2, 1);
            assert test.write(null, 3, 2);
            assert test.write(null, 1, 3);
            assert test.write(null, 3, 4);
            assert test.write(null, 2, 5);
            storages = test.getNodeStorages();
            storages.assertValid();

            test.crash(1);
            storages = test.getNodeStorages();
            storages.assertValid(Set.of(1));
            assert test.join(6);
            storages = test.getNodeStorages();
            storages.assertValid(Set.of(1));

            assert test.recover(1);

            // Still valid
            storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(2, 1);
            storages.assertLatest(3, 1);
            storages.assertLatest(1, 0);
        }
    }

}
