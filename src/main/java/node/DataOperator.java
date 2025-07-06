package node;

import messages.ClientMsgs;

public class DataOperator {
    // Value of null means the thing doesn't exits
    // Useful during creation
    private String value;
    private int version;
    private Multicaster multicaster;

    public DataOperator(Multicaster multicaster) {
        this.multicaster = multicaster;
        this.version = 0;
    }

    public void handle(ClientMsgs.DataMsg msg) {
        // TODO implement
    }
}
