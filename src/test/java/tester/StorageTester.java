package tester;

import node.DataElement;
import utils.Config;
import utils.Pair;

import java.util.HashMap;
import java.util.Map;

public record StorageTester(Map<Integer, Map<Integer, DataElement>> nodesData) {
    public StorageTester(Map<Integer, Map<Integer, DataElement>> nodesData) {
        this.nodesData = nodesData;
        testConsistency();
    }

    private void testConsistency() {
        assertNoNullKey();
        assertNoNegativeVersion();
        assertNoLockLost();
        assertNewVersionW();
    }

    private void assertNoNullKey() {
        assert !this.nodesData.containsKey(null) : "Group contains null key";

        for (var elem : this.nodesData.entrySet()) {
            int nodeId = elem.getKey();
            Map<Integer, DataElement> storage = elem.getValue();
            assert !storage.containsKey(null) : "Node " + nodeId + "contains null key";
        }
    }

    private void assertNoNegativeVersion() {
        for (var elem : this.nodesData.entrySet()) {
            int nodeId = elem.getKey();
            Map<Integer, DataElement> storage = elem.getValue();

            var invalids = storage.values().stream().filter(val -> val.getVersion() < 0);
            assert invalids.findAny().isEmpty() : "Nodes " + nodeId + "contains negative version number";
        }
    }

    private void assertNoLockLost() {
        for (var elem : this.nodesData.entrySet()) {
            int nodeId = elem.getKey();
            Map<Integer, DataElement> storage = elem.getValue();

            var invalids = storage.values().stream().filter(val -> val.isReadLocked() || val.isWriteLocked());
            assert invalids.findAny().isEmpty() : "Nodes " + nodeId + "contains negative locked read or write";
        }
    }

    private void assertNewVersionW() {
        // Contains key -> maxVersion, count
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

    public void assertLatest(int key, int version) {
        int nLatest = 0;
        for (var nodeStorage : nodesData.values()) {
            DataElement elem = nodeStorage.get(key);
            if (elem == null)
                continue;

            assert elem.getVersion() <= version : "There is an unexpected newer version";

            if (elem.getVersion() == version)
                nLatest++;
        }

        assert nLatest >= Config.W : "Latest version of key " + key + " has broken quorum";
    }
}