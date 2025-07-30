package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;


public class Ring<T> {
    static class RingNode<V> {
        public V value;
        public RingNode<V> left;
        public RingNode<V> right;

        public RingNode(V value, RingNode<V> left, RingNode<V> right) {
            this.value = value;
            this.left = left;
            this.right = right;
        }
    }

    private final TreeMap<Integer, RingNode<T>> ring;

    public Ring() {
        ring = new TreeMap<>();
    }

    public T get(int key) {
        var elem = ring.get(key);
        return elem == null ? null : elem.value;
    }

    public void replaceAll(HashMap<Integer, T> newValues) {
        ring.clear();
        for (var entry : newValues.entrySet())
            this.put(entry.getKey(), entry.getValue());
    }

    public Integer getFloorKey(int key) {
        Integer floorKey = ring.floorKey(key);
        if (floorKey == null)
            floorKey = ring.lastKey();
        return floorKey;
    }

    public Integer getCeilKey(int key) {
        Integer ceilKey = ring.ceilingKey(key);
        if (ceilKey == null)
            ceilKey = ring.firstKey();
        return ceilKey;
    }

    public void put(int key, T value) {
        assert key >= 0;

        if (ring.isEmpty()) {
            var node = new RingNode<>(value, null, null);
            node.left = node;
            node.right = node;
            ring.put(key, node);
            return;
        } else if (ring.containsKey(key)) {
            ring.get(key).value = value;
            return;
        }

        // Get lower and upper of a single
        int floorKey = getFloorKey(key);
        int ceilKey = getCeilKey(key);
        RingNode<T> floor = ring.get(floorKey);
        RingNode<T> ceil = ring.get(ceilKey);

        // Connect them and insert them
        RingNode<T> elem = new RingNode<>(value, floor, ceil);
        floor.right = elem;
        ceil.left = elem;
        ring.put(key, elem);
    }

    public void remove(int key) {
        RingNode<T> old = ring.remove(key);
        if (old == null || ring.isEmpty())
            return;

        old.left.right = old.right;
        old.right.left = old.left;
    }

    public HashMap<Integer, T> getHashMap() {
        HashMap<Integer, T> res = new HashMap<>();
        for (var entry : ring.entrySet())
            res.put(entry.getKey(), entry.getValue().value);
        return res;
    }

    public int size() {
        return ring.size();
    }

    private List<T> getPartialInterval(RingNode<T> start, int size, boolean goingRight) {
        ArrayList<T> res = new ArrayList<>();
        RingNode<T> curr = start;
        for (int i = 0; i < size && i < ring.size(); i++) {
            curr = goingRight ? curr.right : curr.left;
            res.add(curr.value);
        }
        return res;
    }

    public List<T> getInterval(int key, int leftSize, int rightSize) {
        RingNode<T> startPoint = ring.get(key);
        if (startPoint == null)
            return null;

        List<T> leftList = getPartialInterval(startPoint, leftSize, false);
        List<T> rightList = getPartialInterval(startPoint, rightSize, true);

        ArrayList<T> res = new ArrayList<>();
        res.addAll(leftList);
        res.addAll(rightList);
        res.add(startPoint.value);

        return res.stream().distinct().toList();
    }

    /// Is inclusive
    public int circularDistance(int start, int end) {
        RingNode<T> curr = ring.get(start);
        RingNode<T> endPoint = ring.get(end);
        assert curr != null && endPoint != null;

        int counter = 1;
        while (curr != endPoint) {
            curr = curr.right;
            counter++;
        }

        return counter;
    }
}
