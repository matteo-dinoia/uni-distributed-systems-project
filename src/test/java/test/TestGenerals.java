package test;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.junit.ClassRule;
import org.junit.Test;
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
            tester.getNodeStorages().assertValid();
        }
    }

    @Test(expected = RuntimeException.class)
    public void invalidCreation() {
        try (Tester tester = new Tester(testKit, Set.of(1))) {
            Utils.ignore(tester);
        }
    }


}
