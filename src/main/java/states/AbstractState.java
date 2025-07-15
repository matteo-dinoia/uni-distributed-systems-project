package states;

import akka.actor.ActorRef;
import messages.client.DataMsg;
import messages.client.StatusMsg;
import messages.node_operation.NodeMsg;
import node.DataElement;
import node.DataStorage;
import node.MemberManager;
import node.Node;

import java.io.Serializable;
import java.util.HashMap;

@SuppressWarnings("unused")
// Return new state if it exist (else return this which mean no change)
public abstract class AbstractState {
    protected final MemberManager members;
    protected final DataStorage storage;
    protected final Node node;
    private ActorRef sender = null;

    protected AbstractState(Node node) {
        this.node = node;
        this.storage = node.storage();
        this.members = node.members();
    }

    // UTILS ---------------------------------------------------------------

    protected AbstractState panic() {
        System.out.println("Something catastrofic happened");
        System.exit(1);
        return null;
    }

    protected AbstractState log_unhandled() {
        System.out.println("Something not handled explicitly happened");
        return this;
    }

    protected AbstractState ignore() {
        return this;
    }

    protected AbstractState keepSameState() {
        return this;
    }

    protected ActorRef sender() {
        return this.sender;
    }

    // DISPATCHER  ---------------------------------------------------------------------

    public final AbstractState handle(ActorRef sender, Serializable message) {
        this.sender = sender;
        return this.handle(message);
    }

    // Overriding this ignore all default handler
    public AbstractState handle(Serializable message) {
        return switch (message) {
            case StatusMsg.Recover msg -> handleRecover(msg);
            case DataMsg.GetMsg msg -> handleGet(msg);
            case NodeMsg.BootstrapRequest msg -> handleBootstrapRequest(msg);
            case NodeMsg.BootstrapResponse msg -> handleBootstrapResponse(msg);
            case NodeMsg.ResponsabilityRequest msg -> handleResponsabilityRequest(msg);
            case NodeMsg.ResponsabilityResponse msg -> handleResponsabilityResponse(msg);
            case StatusMsg.Join msg -> handleJoin(msg);
            case StatusMsg.InitialMembers msg -> handleInitialMembers(msg);
            case NodeMsg.Timeout msg -> handleTimeout(msg);
            default -> throw new IllegalStateException("Unexpected value: " + message);
        };
    }


    // MESSAGE HANDLERS ---------------------------------------------------------------
    protected AbstractState handleRecover(StatusMsg.Recover msg) {
        return panic();
    }

    protected AbstractState handleBootstrapResponse(NodeMsg.BootstrapResponse msg) {
        return panic();
    }

    protected AbstractState handleGet(DataMsg.GetMsg msg) {
        return log_unhandled();
    }

    protected AbstractState handleResponsabilityRequest(NodeMsg.ResponsabilityRequest msg) {
        return panic();
    }

    protected AbstractState handleResponsabilityResponse(NodeMsg.ResponsabilityResponse msg) {
        return panic();
    }

    protected AbstractState handleTimeout(NodeMsg.Timeout msg) {
        return ignore();
    }

    // Custom handler

    protected AbstractState handleAskForMyResponsibility(NodeMsg.ResponsabilityRequest msg) {
        HashMap<Integer, DataElement> toSend = new HashMap<>();
        for (Integer key : storage.getAllKeys()) {
            if (members.isResponsible(msg.requester(), key)) {
                toSend.put(key, storage.get(key));
            }
        }

        members.sendTo(msg.requester(), new NodeMsg.ResponsabilityResponse(msg.requestId(), toSend));
        return this;
    }

    protected AbstractState handleBootstrapRequest(NodeMsg.BootstrapRequest req) {
        HashMap<Integer, ActorRef> currentMembers = members.getMemberList();
        members.sendTo(sender(), new NodeMsg.BootstrapResponse(req.requestId(), currentMembers));
        return keepSameState();
    }

    protected AbstractState handleInitialMembers(StatusMsg.InitialMembers msg) {
        return panic();
    }

    protected AbstractState handleJoin(StatusMsg.Join msg) {
        return panic();
    }
}




