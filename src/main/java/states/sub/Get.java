package states.sub;

import akka.actor.typed.ActorRef;
import messages.Message;
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

    private final ActorRef<Message> client;
    private final HashSet<ActorRef<Message>> respondedPositively = new HashSet<>();
    private final HashSet<ActorRef<Message>> respondedNegatively = new HashSet<>();
    private final Integer lastVersionSeen;
    private DataElement latest = null;

    public Get(Node node, ActorRef<Message> client, DataMsg.Get msg, int requestId) {
        super(node);
        this.requestId = requestId;
        this.client = client;
        this.key = msg.key();
        this.lastVersionSeen = msg.last_version_seen();

        handleInitialMsg(msg);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.SUB;
    }

    public void handleInitialMsg(DataMsg.Get msg) {
        members.sendToDataResponsible(msg.key(), new NodeDataMsg.ReadRequest(requestId, msg.key()));
        members.scheduleSendTimeoutToMyself(requestId);
    }

    @Override
    protected AbstractState handleReadResponse(NodeDataMsg.ReadResponse msg) {
        System.out.println("LOL HEER");
        if (msg.requestId() != requestId) return ignore();

        respondedPositively.add(sender());

        if (latest == null || latest.getVersion() < msg.element().getVersion())
            latest = msg.element();

        if (checkFinished())
            return new Normal(super.node);
        return keepSameState();
    }

    @Override
    protected AbstractState handleReadImpossibleForLock(NodeDataMsg.ReadImpossibleForLock msg) {
        if (msg.requestId() != requestId) return ignore();

        respondedNegatively.add(sender());
        if (Config.N - respondedNegatively.size() < Config.R) {
            members.sendTo(client, new ResponseMsgs.ReadResultFailed(key));
            return new Normal(super.node);
        }
        return keepSameState();
    }

    @Override
    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != requestId) return ignore();

        members.sendTo(client, new ResponseMsgs.ReadTimeout(key));
        return new Normal(super.node);
    }

    private boolean checkFinished() {
        System.out.println("COmpleted " + respondedPositively.size());
        if (respondedPositively.size() < Config.R)
            return false;

        if (latest == null || (lastVersionSeen != null && latest.getVersion() < lastVersionSeen))
            members.sendTo(client, new ResponseMsgs.ReadResultFailed(key));
        else if (latest.getVersion() < 0) // case of entry not existent
            members.sendTo(client, new ResponseMsgs.ReadResultInexistentValue(key));
        else
            members.sendTo(client, new ResponseMsgs.ReadSucceeded(key, latest.getValue(), latest.getVersion()));
        return true;

    }

}
