package states.sub;

import akka.actor.ActorRef;
import messages.client.DataMsg;
import messages.client.ResponseMsgs;
import messages.node_operation.NodeDataMsg;
import messages.node_operation.NodeMsg;
import node.DataElement;
import node.Node;
import node.NodeState;
import states.AbstractState;
import states.Normal;
import utils.Config;

import java.util.HashSet;

public class Get extends AbstractState {
    private final int requestId;
    private final int key;
    private final Integer lastVersionSeen;
    private final ActorRef client;
    private final HashSet<ActorRef> responded = new HashSet<>();
    private DataElement latest = null;

    public Get(Node node, ActorRef client, DataMsg.Get msg) {
        super(node);
        this.requestId = node.getFreshRequestId();
        this.client = client;
        this.lastVersionSeen = msg.last_version_seen();
        this.key = msg.key();

        handleInitialMsg(msg);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.OTHER;
    }

    public void handleInitialMsg(DataMsg.Get msg) {
        members.sendToDataResponsible(msg.key(), new NodeDataMsg.ReadRequest(requestId, msg.key()));
        members.scheduleSendTimeoutToMyself(requestId);
    }

    @Override
    protected AbstractState handleReadReply(NodeDataMsg.ReadReply msg) {
        if (msg.requestId() != requestId) return ignore();

        responded.add(sender());

        if (latest == null || latest.getVersion() < msg.element().getVersion())
            latest = msg.element();

        return checkFinished() ? new Normal(super.node) : keepSameState();
    }

    @Override
    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != requestId) return ignore();

        members.sendTo(client, new ResponseMsgs.ReadTimeout(key));
        return new Normal(super.node);
    }

    private boolean checkFinished() {
        if (responded.size() < Config.R)
            return false;

        if (latest == null || latest.getVersion() < lastVersionSeen)
            members.sendTo(client, new ResponseMsgs.ReadResultFailed(key));
        else
            members.sendTo(client, new ResponseMsgs.ReadResult(key, latest.getValue(), latest.getVersion()));
        return true;

    }

}
