package node;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import messages.client.DataMsg;
import messages.client.StatusMsg;

import java.util.HashMap;

@SuppressWarnings("unused")
public class Node extends AbstractActor {
    private enum NodeState {
        TO_START,
        ALIVE,
        CRASHED,
        JOINING,
        LEAVING,
        RECOVERING;

        boolean isValidChange(NodeState nextState) {
            if (this == ALIVE) {
                return nextState == NodeState.CRASHED || nextState == NodeState.JOINING;
            }
            // TODO
            return false;
        }
    }

    private NodeState state;
    private final MemberManager memberManager;
    // Map client to its own controller
    private final HashMap<ActorRef, Actionator> actionators;
    // Map key to value and its own operator
    private final HashMap<Integer, DataOperator> data;

    public Node(int nodeId) {
        this.memberManager = new MemberManager(nodeId, self());
        this.actionators = new HashMap<>();
        this.data = new HashMap<>();
        this.state = NodeState.TO_START;
    }

    public static Props props(int nodeId) {
        return Props.create(Node.class, () -> new Node(nodeId));
    }


    Actionator getActionator(int requestId) {
        Actionator actionator = actionators.putIfAbsent(sender(), new Actionator(requestId, this.memberManager));
        assert actionator != null;
        return actionator;
    }

    // TODO
    @Override
    public Receive createReceive() {
        switch (state) {
            case STANDARD -> {
            }
            case CRASHED -> {
            }
            case JOINING -> {
            }
            case LEAVING -> {
            }
            case RECOVERING -> {
            }
        }
        return receiveBuilder()
                // handling client.DataMsg
                .match(DataMsg.GetMsg.class, (msg) -> getActionator(msg.requestId).handleGet(sender(), msg))
                .match(DataMsg.UpdateMsg.class, (msg) -> getActionator(msg.requestId).handleUpdate(sender(), msg))
                // handling client.StatusMsg
                .match(StatusMsg.Join.class, this::handleJoin)
                .match(StatusMsg.Leave.class, this::handleLeave)
                .match(StatusMsg.Crash.class, this::handleCrash)
                .match(StatusMsg.InitialMembers.class, this::handleInitialMembers)
                // TODO fix For now invalid client (main) message are dropped
                .build();
    }

    private void changeState(NodeState nextState) {
        // TODO instead of crashing, return invalid
        assert this.state.isValidChange(nextState);
        this.state = nextState;
        getContext().become(createReceive());
    }

    private void handleCrash(StatusMsg.Crash msg) {
        changeState(NodeState.CRASHED);
    }

    private void handleRecover(StatusMsg.Recover msg) {
        changeState(NodeState.RECOVERING);

    }

    private void handleInitialMembers(StatusMsg.InitialMembers msg) {
        changeState(NodeState.ALIVE);
        this.memberList = msg.initial;
    }

    private void handleJoin(StatusMsg.Join msg) {
        changeState(NodeState.JOINING);
    }

    private void handleLeave(StatusMsg.Leave msg) {
        changeState(NodeState.LEAVING);
        //TODO
    }

    private void handleRecover(StatusMsg.Recover msg) {
        changeState(NodeState.RECOVERING);

    }
}
