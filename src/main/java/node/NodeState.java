package node;

public enum NodeState {
    TO_START,
    ALIVE,
    CRASHED,
    JOINING,
    LEAVING,
    RECOVERING,
    LEFT,
    OTHER;

    public boolean isValidChange(NodeState nextState) {
        return switch (this) {
            case TO_START -> nextState == NodeState.JOINING || nextState == NodeState.ALIVE;
            case ALIVE -> nextState == NodeState.CRASHED || nextState == NodeState.LEAVING;
            case CRASHED -> nextState == NodeState.RECOVERING;
            case JOINING, RECOVERING -> nextState == NodeState.ALIVE;
            case LEAVING -> nextState == NodeState.ALIVE || nextState == NodeState.LEFT;
            default -> false;
        };
    }
}