package node;

public record SendableData(String value, int version) {
    public record Debug(String value, int version, DataElement.LockStatus lockStatus) {

    }
}
