package node;

@SuppressWarnings("unused")
public class DataOperator {
    // Value of null means the thing doesn't exits
    // Useful during creation
    private String value;
    private int version;
    private final Multicaster multicaster;

    public DataOperator(Multicaster multicaster) {
        this.multicaster = multicaster;
        this.version = 0;
    }


}
