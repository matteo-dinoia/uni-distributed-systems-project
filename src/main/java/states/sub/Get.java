package states.sub;

import actor.NodeState;
import actor.node.Node;
import actor.node.storage.SendableData;
import akka.actor.typed.ActorRef;
import messages.Message;
import messages.client.DataMsg;
import messages.client.ResponseMsgs;
import messages.node_operation.NodeDataMsg;
import messages.node_operation.NodeMsg;
import states.AbstractState;
import states.Left;
import utils.Config;

import java.util.HashSet;

public class Get extends AbstractState {
    private final int requestId;
    private final int key;

    private final ActorRef<Message> client;
    private final HashSet<ActorRef<Message>> respondedPositively = new HashSet<>();
    private final HashSet<ActorRef<Message>> respondedNegatively = new HashSet<>();
    private final Integer lastVersionSeen;
    private SendableData latest = null;

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
        node.sendToResponsible(msg.key(), new NodeDataMsg.ReadRequest(requestId, msg.key()));
        node.scheduleTimeout(requestId);
    }

    @Override
    protected AbstractState handleReadResponse(NodeDataMsg.ReadResponse msg) {
        if (msg.requestId() != requestId) return ignore();

        respondedPositively.add(sender());

        if (latest == null || latest.version() < msg.element().version())
            latest = msg.element();

        if (checkFinished())
            return new Left(super.node);
        return keepSameState();
    }

    @Override
    protected AbstractState handleReadImpossibleForLock(NodeDataMsg.ReadImpossibleForLock msg) {
        if (msg.requestId() != requestId) return ignore();

        respondedNegatively.add(sender());
        if (Config.N - respondedNegatively.size() < Config.R) {
            node.sendTo(client, new ResponseMsgs.ReadResultFailed(key));
            return new Left(super.node);
        }
        return keepSameState();
    }

    @Override
    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        if (msg.operationId() != requestId) return ignore();

        node.sendTo(client, new ResponseMsgs.ReadTimeout(key));
        return new Left(super.node);
    }

    private boolean checkFinished() {
        if (respondedPositively.size() < Config.R)
            return false;

        if (latest == null || (lastVersionSeen != null && latest.version() < lastVersionSeen))
            node.sendTo(client, new ResponseMsgs.ReadResultFailed(key));
        else if (latest.version() < 0) // case of entry not existent
            node.sendTo(client, new ResponseMsgs.ReadResultInexistentValue(key));
        else
            node.sendTo(client, new ResponseMsgs.ReadSucceeded(key, latest.value(), latest.version()));
        return true;

    }

}
