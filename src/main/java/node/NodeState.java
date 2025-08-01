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
            case JOINING -> nextState == NORMAL || nextState == TO_START;
            case SUB -> nextState == NORMAL;
            case RECOVERING -> nextState == NORMAL || nextState == CRASHED;
            case LEAVING -> nextState == NORMAL || nextState == LEFT;
            default -> false;
        };
    }
}