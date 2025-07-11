package states;

import akka.actor.ActorRef;
import messages.node_operation.NodeMsg;
import node.DataStorage;
import node.MemberManager;

import java.util.HashMap;

public class Recovering extends AbstractState {
    public Recovering(DataStorage storage, MemberManager memberManager) {
        super(storage, memberManager);
    }

    @Override
    protected AbstractState handleBootstrapRequest(NodeMsg.BootstrapRequest req) {
        HashMap<Integer, ActorRef> currentMembers = members.getMemberList();
        members.send(sender(), new NodeMsg.BootstrapResponse(req.requestId(), currentMembers));
        return this;
    }

    @Override
    protected AbstractState handleBootstrapResponse(NodeMsg.BootstrapResponse msg) {
        members.setMemberList(msg.updatedMembers());
        storage.discardKeysNotUnderResponsibility(members);
        return new Normal(storage, members);
    }

}