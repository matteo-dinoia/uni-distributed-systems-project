package node;

import java.io.Serializable;
import java.util.Objects;

public class DataElement implements Serializable {
    private String value;
    private int version;
    private boolean versionReadLocked;

    public DataElement() {
        this.value = null;
        this.version = -1;
        this.versionReadLocked = false;
    }

    public DataElement(String value, int version) {
        this.value = value;
        this.version = version;
        this.versionReadLocked = false;
    }

    public String getValue() {
        return value;
    }

    public int getVersion() {
        return version;
    }

    public boolean isVersionReadLocked() {
        return versionReadLocked;
    }

    public void setValue(String value, int version) {
        this.value = value;
        this.version = version;
    }

    public void setVersionReadLocked(boolean locked) {
        this.versionReadLocked = locked;
    }

    @Override
    public String toString() {
        return "DataElement{" +
                "value='" + value + '\'' +
                ", version=" + version +
                ", locked=" + versionReadLocked +
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
