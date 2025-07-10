package states;

import node.DataStorage;
import node.MemberManager;

public class Leaving extends AbstractState {
    public Leaving(DataStorage storage, MemberManager memberManager) {
        super(storage, memberManager);
    }
}
