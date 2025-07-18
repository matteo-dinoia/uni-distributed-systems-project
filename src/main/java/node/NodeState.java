package node;

// TODO IGNORE
public enum NodeState {
    TO_START,
    ALIVE,
    CRASHED,
    JOINING,
    LEAVING,
    RECOVERING,
    LEFT,
    OTHER;

    // TODO use baseState and instance of and maybe put in base state (only if we decide to keep it)
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