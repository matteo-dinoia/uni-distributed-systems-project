package states;

import node.DataStorage;
import node.MemberManager;

public class Normal extends AbstractState {
    // Map client to its own controller
    //private final HashMap<ActorRef, Actionator> actionators;

    public Normal(DataStorage storage, MemberManager memberManager) {
        super(storage, memberManager);
    }
}
