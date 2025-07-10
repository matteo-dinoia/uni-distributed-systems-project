package states;

import node.DataStorage;
import node.MemberManager;

public class Joining extends AbstractState {
    public Joining(DataStorage storage, MemberManager memberManager) {
        super(storage, memberManager);
    }
}
