package node;

import java.io.Serializable;
import java.util.Objects;

public class DataElement implements Serializable {
    private String value;
    private int version;
    private boolean writeLocked;
    private boolean readLocked;

    public DataElement() {
        this.value = null;
        this.version = -1;
        this.writeLocked = false;
        this.readLocked = false;
    }

    public DataElement(DataElement toCopy) {
        this.value = toCopy.value;
        this.version = toCopy.version;
        this.writeLocked = toCopy.writeLocked;
        this.readLocked = toCopy.readLocked;
    }

    /**
     * Returns the stored value, unless the element is locked for writing or read-locked.
     *
     * @throws IllegalStateException if a read or write lock is held
     */
    public String getValue() {
        if (writeLocked)
            throw new IllegalStateException("Cannot read while write-lock is held");
        if (readLocked)
            throw new IllegalStateException("DataElement is read-locked");
        return value;
    }

    public int getVersion() {
        return version;
    }

    public boolean isReadLocked() {
        return readLocked;
    }

    public void setReadLocked(boolean locked) {
        this.readLocked = locked;
    }

    public boolean isWriteLocked() {
        return writeLocked;
    }

    public void setWriteLocked(boolean locked) {
        this.writeLocked = locked;
    }

    /**
     * Updates the value and version, only if a write-lock is held.
     * throws IllegalStateException if no write-lock is held
     */
    public void updateValue(String newValue, int newVersion) {
        if (!writeLocked)
            throw new IllegalStateException("Cannot write without acquiring write-lock first");

        this.value = newValue;
        this.version = newVersion;
    }

    @Override
    public String toString() {
        return "DataElement{" +
                "value='" + value + '\'' +
                ", version=" + version +
                ", readLocked=" + readLocked +
                ", writeLocked=" + writeLocked +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataElement that)) return false;
        return version == that.version && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, version);
    }
}
