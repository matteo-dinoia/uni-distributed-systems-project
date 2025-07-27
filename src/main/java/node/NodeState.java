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
        return switch (this) {
            case TO_START -> nextState == NodeState.JOINING || nextState == NodeState.NORMAL;
            case NORMAL -> nextState == NodeState.CRASHED || nextState == NodeState.LEAVING;
            case CRASHED -> nextState == NodeState.RECOVERING;
            case JOINING, RECOVERING, SUB -> nextState == NodeState.NORMAL;
            case LEAVING -> nextState == NodeState.NORMAL || nextState == NodeState.LEFT;
            default -> false;
        };
    }
}