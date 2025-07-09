package states;

import java.io.Serializable;

public abstract class Base implements Serializable {
    public final int requestId;

    protected Base(requestId){
        this.requestId = requestId;
    }

    // Return new state
    public abstract Base handle(System system);
}
