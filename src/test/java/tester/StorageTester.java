package tester;

import actor.node.storage.DataElement;
import actor.node.storage.SendableData;
import utils.Config;
import utils.structs.Pair;
import utils.structs.Ring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.System.out;

public record StorageTester(Map<Integer, Map<Integer, SendableData.Debug>> nodesData) {
    public void assertValid() {
        assertValid(new HashSet<>());
    }

    /// Use crash node list to know which to ignore for
    /// the test in which we check responsability drop
    /// because it is only dropped after recovery.
    public void assertValid(Set<Integer> crashedNode) {
        assertNoNullKey();
        assertNoValueOutsideResponsability(crashedNode);
        assertNoNegativeVersion();
        assertNoLockLost();
        assertNewVersionQuorum();
    }

    public void assertNoNullKey() {
        assert !this.nodesData.containsKey(null) : "Group contains null key";

        for (var elem : this.nodesData.entrySet()) {
            int nodeId = elem.getKey();
            var storage = elem.getValue();
            assert !storage.containsKey(null) : "Node " + nodeId + "contains null key";
        }
    }

    public void assertNoNegativeVersion() {
        for (var elem : this.nodesData.entrySet()) {
            int nodeId = elem.getKey();
            var storage = elem.getValue();

            var invalids = storage.values().stream().filter(val -> val.version() < 0);
            assert invalids.findAny().isEmpty() : "Nodes " + nodeId + " contains negative version number";
        }
    }

    public void assertNoLockLost() {
        for (var elem : this.nodesData.entrySet()) {
            int nodeId = elem.getKey();
            var storage = elem.getValue();

            var invalids = storage.values().stream().filter(val -> (val.lockStatus() != DataElement.LockStatus.FREE) && val.version() >= 0);
            assert invalids.findAny().isEmpty() : "Nodes " + nodeId + " contains values locked on read or on write";
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isResponsabile(Ring<Integer> group, int key, Integer node) {
        var responsible = group.getInterval(group.getCeilKey(key), 0, Config.N - 1);
        return responsible.contains(node);
    }

    private Ring<Integer> getRing() {
        HashMap<Integer, Integer> hash = new HashMap<>();
        for (Integer node : nodesData.keySet())
            hash.put(node, node);

        return new Ring<>(hash);
    }

    public void assertNoValueOutsideResponsability(Set<Integer> crashedNode) {
        Ring<Integer> ring = getRing();

        for (var elem : this.nodesData.entrySet()) {
            Integer nodeId = elem.getKey();
            // Skip crashed node (which are allowed to have data not under their responsability)
            // they will drop them on recover
            if (crashedNode.contains(nodeId))
                continue;

            var storage = elem.getValue();

            var invalids = storage.keySet().stream().filter(key -> !isResponsabile(ring, key, nodeId));
            var invalidList = invalids.toList();
            assert invalidList.isEmpty() : "Nodes " + nodeId + " contains values outside its responsability " + invalidList;
        }
    }

    private void assertNewVersionQuorum() {
        Ring<Integer> ring = getRing();
        // Contains key -> maxVersion, 3 contains values outside its responsability [2] count
        Map<Integer, Pair<Integer, Integer>> maxVersionPerKey = new HashMap<>();

        for (var entry2 : this.nodesData.entrySet()) {
            var nodeId = entry2.getKey();
            var storage = entry2.getValue();


            for (var entry : storage.entrySet()) {
                int key = entry.getKey();
                int version = entry.getValue().version();

                // Only valid for some node crashed
                if (!isResponsabile(ring, key, nodeId))
                    continue;

                var max = maxVersionPerKey.get(key);
                if (max == null || max.getLeft() < version)
                    maxVersionPerKey.put(key, new Pair<>(version, 1));
                else if (max.getLeft() == version)
                    max.setRight(max.getRight() + 1);
            }
        }

        for (var max : maxVersionPerKey.entrySet()) {
            int key = max.getKey();
            int count = max.getValue().getRight();
            assert count >= Config.W : "Quorum not reached or lost on key " + key;
        }
    }

    // MANUAL CHECKERS

    public void assertLatest(int key, int version) {
        if (version < 0) {
            assertMissing(key);
            return;
        }


        int nLatest = 0;
        int maxVersion = -1;
        for (var nodeStorage : nodesData.values()) {
            SendableData.Debug elem = nodeStorage.get(key);
            if (elem == null)
                continue;

            if (maxVersion < elem.version()) {
                nLatest = 1;
                maxVersion = elem.version();
            } else if (maxVersion <= elem.version()) {
                nLatest++;
            }

        }

        assert maxVersion == version : "The current version is " + maxVersion + " while expecting " + version;
        assert nLatest >= Config.W : "Latest version of key " + key + " has broken quorum (or wrong version)";
    }

    public void assertMissing(int key) {
        for (var elem : this.nodesData.entrySet()) {
            int nodeId = elem.getKey();
            var storage = elem.getValue();
            assert !storage.containsKey(key) : "Node " + nodeId + "contains key " + key + " which should be missing";
        }
    }

    // UTILS

    @SuppressWarnings("unused")
    public void printKeyStatus(int key) {
        out.println();
        out.println("STATUS OF KEY " + key + " is: ");
        for (var elem : this.nodesData.entrySet()) {
            int nodeId = elem.getKey();
            var storage = elem.getValue();
            out.println(" |- " + nodeId + " -> " + storage.get(key) + " ");
        }
        out.println();
    }

    @SuppressWarnings("unused")
    public void printAlive() {
        out.print("STILL ALIVE ARE ");
        for (var nodeId : this.nodesData.keySet())
            out.print(" " + nodeId);
        out.println();
    }
}