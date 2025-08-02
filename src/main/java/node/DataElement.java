package node;

import akka.actor.typed.ActorRef;
import messages.Message;
import utils.Pair;

import java.io.Serializable;

public class DataElement implements Serializable {
    public enum LockStatus {FREE, WRITE_LOCKED, READ_WRITE_LOCKED}

    private String value;
    private int version;
    private LockStatus lockStatus;
    private Pair<ActorRef<Message>, Integer> lockHolder;

    public DataElement() {
        this(null, -1);
    }

    public DataElement(String value, int version) {
        this.value = value;
        this.version = version;
        this.lockStatus = LockStatus.FREE;
        this.lockHolder = null;
    }

    public SendableData sendable() {
        assert !isReadLocked() : "Trying to read when read locked";
        return new SendableData(this.value, this.version);
    }

    public SendableData.Debug debugSendable() {
        return new SendableData.Debug(this.value, this.version, this.lockStatus);
    }

    public int getVersion() {
        return version;
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isLockedBy(ActorRef<Message> node, int reqId) {
        return this.lockHolder != null && lockHolder.equals(new Pair<>(node, reqId));
    }

    public boolean isWriteLocked() {
        return this.lockStatus != LockStatus.FREE;
    }

    /// @return whether or not it could write lock
    public boolean writeLock(ActorRef<Message> node, int reqId) {
        if (!this.isFree())
            return false;
        this.lockHolder = new Pair<>(node, reqId);
        this.lockStatus = LockStatus.WRITE_LOCKED;
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isReadLocked() {
        return this.lockStatus == LockStatus.READ_WRITE_LOCKED;
    }

    /// @return whether or not it could read lock
    public boolean readLock(ActorRef<Message> node, int reqId) {
        if (!isLockedBy(node, reqId) || this.lockStatus != LockStatus.WRITE_LOCKED)
            return false;
        this.lockStatus = LockStatus.READ_WRITE_LOCKED;
        return true;

    }

    public boolean isFree() {
        return this.lockStatus == LockStatus.FREE;
    }

    /// @return whether or not it could free locks
    public boolean freeLocks(ActorRef<Message> node, int reqId) {
        if (!isLockedBy(node, reqId) || this.lockStatus == LockStatus.FREE)
            return false;

        this.lockHolder = null;
        this.lockStatus = LockStatus.FREE;
        return true;
    }

    /**
     * Updates the value and version, only if a write-lock is held.
     * throws IllegalStateException if no write-lock is held
     */
    public void updateValue(String newValue, int newVersion) {
        this.value = newValue;
        this.version = newVersion;
    }

    @Override
    public String toString() {
        return "DataElement{" +
                "value='" + value + "'" +
                ", version=" + version +
                ", status=" + this.lockStatus +
                ", handler=" + this.lockHolder +
                '}';
    }

}
