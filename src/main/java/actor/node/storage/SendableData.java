package actor.node.storage;

public record SendableData(String value, int version) {
    public record Debug(String value, int version, DataElement.LockStatus lockStatus) {

    }
}
