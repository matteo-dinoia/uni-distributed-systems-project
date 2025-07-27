package node;

import java.io.Serializable;
import java.util.Objects;

public class DataElement implements Serializable {
    private String value;
    private int version;
    private boolean versionReadLocked;
    private boolean lockedForWrite;
    private boolean readLocked;

    public DataElement() {
        this.value = null;
        this.version = -1;
        this.versionReadLocked = false;
        this.lockedForWrite = false;
        this.readLocked = false;
    }

    public DataElement(String value, int version) {
        this.value = value;
        this.version = version;
        this.versionReadLocked = false;
        this.lockedForWrite = false;
        this.readLocked = false;
    }

    /**
     * Returns the stored value, unless the element is locked for writing or read-locked.
     *
     * @throws IllegalStateException if a read or write lock is held
     */
    public String getValue() {
        if (lockedForWrite) {
            throw new IllegalStateException("Cannot read while write-lock is held");
        }
        if (readLocked) {
            throw new IllegalStateException("DataElement is read-locked");
        }
        return value;
    }

    public int getVersion() {
        return version;
    }

    public boolean isVersionReadLocked() {
        return versionReadLocked;
    }

    public void setVersionReadLocked(boolean locked) {
        this.versionReadLocked = locked;
    }

    public boolean isReadLocked() {
        return readLocked;
    }

    public void setReadLocked(boolean locked) {
        this.readLocked = locked;
    }

    public boolean isLockedForWrite() {
        return lockedForWrite;
    }

    public void setLockedForWrite(boolean locked) {
        this.lockedForWrite = locked;
    }

    /**
     * Updates the value and version, only if a write-lock is held.
     * throws IllegalStateException if no write-lock is held
     */
    public void updateValue(String newValue, int newVersion) {
        if (!lockedForWrite) {
            throw new IllegalStateException("Cannot write without acquiring write-lock first");
        }
        this.value = newValue;
        this.version = newVersion;
    }

    @Override
    public String toString() {
        return "DataElement{" +
                "value='" + value + '\'' +
                ", version=" + version +
                ", versionReadLocked=" + versionReadLocked +
                ", readLocked=" + readLocked +
                ", writeLocked=" + lockedForWrite +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataElement)) return false;
        DataElement that = (DataElement) o;
        return version == that.version && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, version);
    }
}
