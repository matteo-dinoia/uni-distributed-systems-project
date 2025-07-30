package node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DataStorage {
    // Map key to value and its own operator
    private final HashMap<Integer, DataElement> data;

    public DataStorage() {
        this.data = new HashMap<>();
    }

    public void discardKeysNotUnderResponsibility(MemberManager members) {
        Set<Integer> keys = new HashSet<>(data.keySet());
        for (Integer key : keys) {
            if (!members.isResponsible(members.getSelfRef(), key)) {
                data.remove(key);
            }
        }
    }

    public void put(int key, DataElement element) {
        data.put(key, element);
    }

    public DataElement get(int key) {
        return data.get(key);
    }

    public Set<Integer> getAllKeys() {
        return data.keySet();
    }

    public void removeIfRepresentNotExistent(int key) {
        DataElement elem = get(key);
        if (elem != null && elem.getVersion() < 0)
            data.remove(key);
    }

    public void putAll(HashMap<Integer, DataElement> toInsert) {
        for (var elem : toInsert.entrySet()) {
            assert elem.getKey() != null : "Inserting a null key";
            put(elem.getKey(), elem.getValue());
        }

    }

    public void removeNotUnderMyControl(MemberManager members) {
        var keySet = this.data.keySet();
        for (Integer key : keySet) {
            assert key != null : "Found a null key";
            if (!members.isResponsible(members.getSelfRef(), key))
                data.remove(key);
        }

    }

    public Map<Integer, DataElement> getCopyOfData() {
        HashMap<Integer, DataElement> copy = new HashMap<>();
        for (var entry : this.data.entrySet())
            copy.put(entry.getKey(), new DataElement(entry.getValue()));
        return copy;
    }
}
