package test.otherParameters;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.junit.ClassRule;
import org.junit.Test;
import tester.Tester;
import utils.Config;

import java.util.Set;

public class DifferentParameters {
    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void write4Crash() {
        Config config = Config.defaultConfig(Config.SHOW_ALL_LOG_IN_TESTS).quorum(9, 5, 4);
        try (Tester test = new Tester(testKit, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9), config)) {
            test.crash(2);
            test.crash(4);
            test.crash(6);
            test.crash(8);

            assert test.write(null, 1, 3);

            var storages = test.getNodeStorages();
            storages.assertValid();
            storages.assertLatest(1, 0);
        }
    }
}
