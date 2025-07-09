package node;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import messages.client.DataMsg;
import messages.client.StatusMsg;
import messages.node_operation.NodeMsg;

import java.util.HashMap;
import java.util.List;

@SuppressWarnings("unused")
public class Node extends AbstractActor {
    // TODO NEED TIMEOUT


    private NodeState state;
    private final MemberManager memberManager;
    private final Multicaster multicaster;
    // Map client to its own controller
    private final HashMap<ActorRef, Actionator> actionators;
    // Map key to value and its own operator
    private final HashMap<Integer, DataOperator> data;

    public Node(int nodeId) {
        this.memberManager = new MemberManager(nodeId, self());
        this.multicaster = this.memberManager;
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
        /* TODO switch (state) {
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
        }*/
        return receiveBuilder()
                // handling client.DataMsg
                .match(DataMsg.GetMsg.class, (msg) -> getActionator(msg.requestId).handleGet(sender(), msg))
                .match(DataMsg.UpdateMsg.class, (msg) -> getActionator(msg.requestId).handleUpdate(sender(), msg))
                // handling client.StatusMsg
                .match(StatusMsg.Join.class, this::handleJoin)
                .match(StatusMsg.Leave.class, this::handleLeave)
                .match(StatusMsg.Crash.class, this::handleCrash)
                .match(StatusMsg.Recover.class, this::handleRecover)
                .match(NodeMsg.BootstrapRequest.class, (msg) -> this.handleBootstrapRequest(sender(), msg))
                .match(NodeMsg.BootstrapResponse.class, (msg) -> this.handleBootstrapResponse(sender(), msg))
                .match(StatusMsg.InitialMembers.class, this::handleInitialMembers)
                // TODO fix For now invalid client (main) message are dropped
                .build();
    }

    private void changeState(NodeState nextState) {
        assert this.state.isValidChange(nextState);
        this.state = nextState;
        getContext().become(createReceive());
    }

    private void handleCrash(StatusMsg.Crash msg) {
        changeState(NodeState.CRASHED);
    }

    private void handleInitialMembers(StatusMsg.InitialMembers msg) {
        changeState(NodeState.ALIVE);
        // TODO
    }

    private void handleJoin(StatusMsg.Join msg) {
        changeState(NodeState.JOINING);
        multicaster.send(msg.bootstrappingPear, new NodeMsg.BootstrapRequest(msg.requestId));
    }


    // ///////////////////// Leaving procedure:
    private void handleLeave(StatusMsg.Leave msg) {
        changeState(NodeState.LEAVING);

        // Per ogni chiave posseduta dal nodo che sta lasciando
        for (var entry : data.entrySet()) {
            int key = entry.getKey();
            DataOperator op = entry.getValue();
            String value = op.get();

            // Ottieni i nodi che saranno responsabili dopo che questo nodo avrà lasciato
            List<ActorRef> futureResponsible = memberManager.getFutureResponsibleFor(key, self());

            for (ActorRef node : futureResponsible) {
                if (!node.equals(getSelf())) {
                    int version = op.getVersion();
                    memberManager.send(node, new DataMsg.UpdateMsg(msg.requestId, key, value, version));
                }
            }
        }
        data.clear();
        getContext().stop(self());
    }
    // ///////////////////// End of Leaving procedure


    // ///////////////////// Recovery procedure:
    private void handleRecover(StatusMsg.Recover msg) {
        changeState(NodeState.RECOVERING);
        multicaster.send(msg.bootstrappingPear, new NodeMsg.BootstrapRequest(msg.requestId));
    }

    private void handleBootstrapRequest(ActorRef sender, NodeMsg.BootstrapRequest msg) {
        // List of active nodes
        HashMap<Integer, ActorRef> currentMembers = memberManager.getMemberList();

        //Aswer back to the recovery node
        multicaster.send(sender, new NodeMsg.BootstrapResponse(msg.requestId, currentMembers));
    }

    private void handleBootstrapResponse(ActorRef _sender, NodeMsg.BootstrapResponse msg) {
        // Update members
        this.memberManager.setMemberList(msg.updatedMembers);

        switch (this.state) {
            case JOINING -> {
                // TODO
            }
            case RECOVERING -> {
                // TODO sdfds
                //        //data that crashed node has to manage
//        HashMap<Integer, String> dataToTransfer = new HashMap<>();
//        for (var entry : data.entrySet()) {
//            int key = entry.getKey();
//
//            if (memberManager.isResponsible(msg.recoveringNode, key)) {
//                dataToTransfer.put(key, entry.getValue().get()); // ///////////////////////////////////////////////////// -> ADDED methods in DataOperator.java
//            }
//        }

//        this.data.clear();
//
//        // Insert new received data
//        for (var entry : msg.dataToTransfer.entrySet()) {
//            this.data.put(entry.getKey(), new DataOperator(entry.getValue()));
//        }
//
//        changeState(NodeState.ALIVE);

            }
            default -> {
                return;
            }
        }


    }
    // ///////////////////// End of Recovery procedure

}
