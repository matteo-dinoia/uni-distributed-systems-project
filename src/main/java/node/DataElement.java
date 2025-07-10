package node;

class DataElement {
    private String value;
    private int version;
    private boolean versionReadLocked;

    public DataElement() {
        this.value = null;
        this.version = -1;
        this.versionReadLocked = false;
    }
}