package node;

import messages.control.ControlMsg;

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

    public void put(int key, SendableData element) {
        data.put(key, new DataElement(element.value(), element.version()));
    }

    public DataElement getOrInsertEmpty(int key) {
        return data.computeIfAbsent(key, k -> new DataElement());
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

    public void refreshIfNeeded(Map<Integer, SendableData> toInsertList) {
        for (var elem : toInsertList.entrySet()) {
            DataElement existing = getOrInsertEmpty(elem.getKey());
            SendableData other = elem.getValue();
            assert other != null : "Someone passed me a null value!";

            if (existing.getVersion() < other.version())
                existing.updateValue(other.value(), other.version());
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

    public ControlMsg.DebugCurrentStorageResponse getDebugInfoMsg(int selfId) {
        HashMap<Integer, SendableData.Debug> toSend = new HashMap<>();
        for (var entry : data.entrySet()) {
            DataElement elem = entry.getValue();
            toSend.put(entry.getKey(), elem.debugSendable());
        }


        return new ControlMsg.DebugCurrentStorageResponse(selfId, toSend);
    }
}
