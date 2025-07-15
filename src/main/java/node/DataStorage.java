package node;

import java.util.HashMap;
import java.util.HashSet;
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
}
