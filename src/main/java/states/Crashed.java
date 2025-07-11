package states;

import messages.client.DataMsg;
import messages.client.StatusMsg;
import node.DataStorage;
import node.MemberManager;

public class Crashed extends AbstractState {
    public Crashed(DataStorage storage, MemberManager memberManager) {
        super(storage, memberManager);
    }

    @Override
    protected AbstractState handleRecover(StatusMsg.Recover msg) {
        Recovering newState = new Recovering(super.storage, super.members);
        //TODO newState.initialMsgSend();
        members.send(msg.bootstrappingPear(), new NodeMsgs.BootstrapRequest(0));
        return newState;
    }

    @Override
    protected AbstractState handleGet(DataMsg.GetMsg msg) {
        return ignore();
    }
}
