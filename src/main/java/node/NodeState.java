package node;

public enum NodeState {
    TO_START,
    ALIVE,
    CRASHED,
    JOINING,
    LEAVING,
    RECOVERING,
    LEFT;

    //TODO switch
    boolean isValidChange(NodeState nextState) {
        if (this == TO_START) {
            return nextState == NodeState.JOINING || nextState == NodeState.ALIVE;
        } else if (this == ALIVE) {
            return nextState == NodeState.CRASHED || nextState == NodeState.LEAVING;
        } else if (this == CRASHED) {
            return nextState == NodeState.RECOVERING;
        } else if (this == JOINING) {
            return nextState == NodeState.ALIVE;
        } else if (this == LEAVING) {
            return nextState == NodeState.ALIVE || nextState == NodeState.LEFT;
        } else if (this == RECOVERING) {
            return nextState == NodeState.ALIVE;
        }

        return false;
    }

}