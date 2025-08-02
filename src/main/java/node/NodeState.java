package node;

public enum NodeState {
    TO_START,
    NORMAL,
    CRASHED,
    JOINING,
    LEAVING,
    RECOVERING,
    LEFT,
    SUB;

    public boolean isValidChange(NodeState nextState) {
        if (this == nextState)
            return true;

        return switch (this) {
            case TO_START -> nextState == JOINING || nextState == NORMAL;
            case NORMAL -> nextState == CRASHED || nextState == LEAVING || nextState == LEFT;
            case CRASHED -> nextState == RECOVERING;
            case JOINING, LEAVING -> nextState == NORMAL || nextState == LEFT;
            case SUB -> nextState == LEFT;
            case RECOVERING -> nextState == NORMAL || nextState == CRASHED;
            default -> false;
        };
    }
}