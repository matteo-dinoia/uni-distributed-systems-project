package states;

import node.DataStorage;
import node.MemberManager;

public class Recovering extends AbstractState {
    public Recovering(DataStorage storage, MemberManager memberManager) {
        super(storage, memberManager);
    }
}