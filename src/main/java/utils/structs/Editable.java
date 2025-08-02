package utils.structs;

public class Editable<V> {
    public V valid;

    public Editable(V valid) {
        this.valid = valid;
    }

    @Override
    public String toString() {
        return "" + this.valid;
    }
}
