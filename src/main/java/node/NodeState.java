package node;

// TODO IGNORE
public enum NodeState {
    TO_START,
    ALIVE,
    CRASHED,
    JOINING,
    LEAVING,
    RECOVERING,
    LEFT;

    // TODO use baseState and instance of and maybe put in base state (only if we decide to keep it)
    boolean isValidChange(NodeState nextState) {
        switch (this) {
            case TO_START:
                return nextState == NodeState.JOINING || nextState == NodeState.ALIVE;

            case ALIVE:
                return nextState == NodeState.CRASHED || nextState == NodeState.LEAVING;

            case CRASHED:
                return nextState == NodeState.RECOVERING;

            case JOINING:
                return nextState == NodeState.ALIVE;

            case LEAVING:
                return nextState == NodeState.ALIVE || nextState == NodeState.LEFT;

            case RECOVERING:
                return nextState == NodeState.ALIVE;

            default:
                return false;
        }
    }

    /* Appunti miei
Join	    TO_START → JOINING → ALIVE
Crash	    ALIVE → CRASHED
Recovery	CRASHED → RECOVERING → ALIVE
Leaving	    ALIVE → LEAVING → LEFT
*/

}