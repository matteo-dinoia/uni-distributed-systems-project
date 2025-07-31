package tester;

import node.DataElement;
import utils.Config;
import utils.Pair;
import utils.Ring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.System.out;

public record StorageTester(Map<Integer, Map<Integer, DataElement>> nodesData) {
    public StorageTester(Map<Integer, Map<Integer, DataElement>> nodesData) {
        this.nodesData = nodesData;
    }

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
            Map<Integer, DataElement> storage = elem.getValue();
            assert !storage.containsKey(null) : "Node " + nodeId + "contains null key";
        }
    }

    // OPTIONAL TODO
    public void assertNoNegativeVersion() {
        for (var elem : this.nodesData.entrySet()) {
            int nodeId = elem.getKey();
            Map<Integer, DataElement> storage = elem.getValue();

            var invalids = storage.values().stream().filter(val -> val.getVersion() < 0);
            assert invalids.findAny().isEmpty() : "Nodes " + nodeId + " contains negative version number";
        }
    }

    public void assertNoLockLost() {
        for (var elem : this.nodesData.entrySet()) {
            int nodeId = elem.getKey();
            Map<Integer, DataElement> storage = elem.getValue();

            var invalids = storage.values().stream().filter(val -> (val.isReadLocked() || val.isWriteLocked()) && val.getVersion() >= 0);
            assert invalids.findAny().isEmpty() : "Nodes " + nodeId + " contains values locked on read or on write";
        }
    }

    private boolean isResponsabile(Ring<Integer> group, int key, Integer node) {
        var responsible = group.getInterval(group.getCeilKey(key), 0, Config.N - 1);
        return responsible.contains(node);
    }

    // TODO CLEAR LATER
    public void assertNoValueOutsideResponsability(Set<Integer> crashedNode) {
        Ring<Integer> group = new Ring<>();
        HashMap<Integer, Integer> hash = new HashMap<>();
        for (Integer node : nodesData.keySet())
            hash.put(node, node);
        group.replaceAll(hash);


        for (var elem : this.nodesData.entrySet()) {
            Integer nodeId = elem.getKey();
            // Skip crashed node (which are allowed to have data not under their responsability)
            // they will drop them on recover
            if (crashedNode.contains(nodeId))
                continue;

            Map<Integer, DataElement> storage = elem.getValue();

            var invalids = storage.keySet().stream().filter(key -> !isResponsabile(group, key, nodeId));
            var invalidList = invalids.toList();
            assert invalidList.isEmpty() : "Nodes " + nodeId + " contains values outside its responsability " + invalidList;
        }
    }

    private void assertNewVersionQuorum() {
        // Contains key -> maxVersion, 3 contains values outside its responsability [2] count
        Map<Integer, Pair<Integer, Integer>> maxVersionPerKey = new HashMap<>();

        for (Map<Integer, DataElement> storage : this.nodesData.values()) {
            for (var entry : storage.entrySet()) {
                int key = entry.getKey();
                int version = entry.getValue().getVersion();

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
            DataElement elem = nodeStorage.get(key);
            if (elem == null)
                continue;

            if (maxVersion < elem.getVersion()) {
                nLatest = 1;
                maxVersion = elem.getVersion();
            } else if (maxVersion <= elem.getVersion()) {
                nLatest++;
            }

        }

        // TODO split in 2 cases
        assert maxVersion == version : "The current version is " + maxVersion + " while expecting " + version;
        assert nLatest >= Config.W : "Latest version of key " + key + " has broken quorum (or wrong version)";
    }

    public void assertMissing(int key) {
        for (var elem : this.nodesData.entrySet()) {
            int nodeId = elem.getKey();
            Map<Integer, DataElement> storage = elem.getValue();
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
            Map<Integer, DataElement> storage = elem.getValue();
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