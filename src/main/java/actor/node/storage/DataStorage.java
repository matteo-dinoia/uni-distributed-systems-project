package actor.node.storage;

import actor.node.MemberManager;
import actor.node.NodeInfo;
import messages.control.ControlMsg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DataStorage {
    // Map key to value and its own operator
    private final HashMap<Integer, DataElement> data;
    private final NodeInfo node;

    public DataStorage(NodeInfo node) {
        this.node = node;
        this.data = new HashMap<>();
    }

    // GETTER AND SETTER

    public DataElement get(int key) {
        return data.get(key);
    }

    public Set<Integer> getAllKeys() {
        return data.keySet();
    }

    public DataElement getOrInsertEmpty(int key) {
        return data.computeIfAbsent(key, _ -> new DataElement());
    }

    public void put(int key, SendableData element) {
        data.put(key, new DataElement(element.value(), element.version()));
    }

    // REMOVER

    public void discardNotResponsible(MemberManager members) {
        Set<Integer> keys = new HashSet<>(data.keySet());
        for (Integer key : keys) {
            if (!members.isResponsible(node.self(), key))
                data.remove(key);
        }
    }

    public void removeIfRepresentNotExistent(int key) {
        DataElement elem = get(key);
        if (elem != null && elem.getVersion() < 0)
            data.remove(key);
    }

    // OTHER

    public void refreshIfNeeded(Map<Integer, SendableData> toInsertList) {
        for (var elem : toInsertList.entrySet()) {
            DataElement existing = getOrInsertEmpty(elem.getKey());
            SendableData other = elem.getValue();

            if (existing.getVersion() < other.version())
                existing.updateValue(other.value(), other.version());
        }
    }

    public ControlMsg.DebugCurrentStorageResponse getDebugInfoMsg(int selfId) {
        HashMap<Integer, SendableData.Debug> toSend = new HashMap<>();
        for (var entry : data.entrySet())
            toSend.put(entry.getKey(), entry.getValue().debugSendable());

        return new ControlMsg.DebugCurrentStorageResponse(selfId, toSend);
    }
}
