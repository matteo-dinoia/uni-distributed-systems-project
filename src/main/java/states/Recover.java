package states;

public abstract class Recover extends Base {
    public static class None extends Recover {

        @Override
        public Base handle(System system) {
            return null;
        }
    }

    public static class Init extends Recover {

        @Override
        public Base handle(System system) {
            return null;
        }
    }

//    public Base initialHandle(System system, ActorRef sender, StatusMsg.Recover msg){ //TODO fix recover name
//        system.setState(Node.NodeState.RECOVERING);
//        system.sendMsg(msg.bootstrappingPear, new NodeMsg.BootstrapRequest(msg.requestId));
//        return new Start();
//    }
//
//    public Base initialHandle(System system, ActorRef sender, RecoverBootstapMsg msg){ //TODO fix recover name
//        system.setState(Node.NodeState.RECOVERING);
//        system.sendMsg(msg.bootstrappingPear, new NodeMsg.BootstrapRequest(msg.requestId));
//        return null;
//    }
//
//
//    public static class Start extends Recover {
//        @Override
//        public Base handle(System system) {
//
//        }
//    }
//
//    private void handleBootstrapRequest(ActorRef sender, NodeMsg.BootstrapRequest msg) {
//        // List of active nodes
//        HashMap<Integer, ActorRef> currentMembers = memberManager.getMemberList();
//
//        //Aswer back to the recovery node
//        multicaster.send(sender, new NodeMsg.BootstrapResponse(msg.requestId, currentMembers));
//    }
//
//    private void handleBootstrapResponse(ActorRef _sender, NodeMsg.BootstrapResponse msg) {
//        // Update members
//        this.memberManager.setMemberList(msg.updatedMembers);
//
//        switch (this.state) {
//            case JOINING -> {
//                // TODO
//            }
//            case RECOVERING -> {
//                // TODO sdfds
//                //        //data that crashed node has to manage
////        HashMap<Integer, String> dataToTransfer = new HashMap<>();
////        for (var entry : data.entrySet()) {
////            int key = entry.getKey();
////
////            if (memberManager.isResponsible(msg.recoveringNode, key)) {
////                dataToTransfer.put(key, entry.getValue().get()); // ///////////////////////////////////////////////////// -> ADDED methods in DataOperator.java
////            }
////        }
//
////        this.data.clear();
////
////        // Insert new received data
////        for (var entry : msg.dataToTransfer.entrySet()) {
////            this.data.put(entry.getKey(), new DataOperator(entry.getValue()));
////        }
////
////        changeState(NodeState.ALIVE);
//
//            }
//            default -> {
//                return;
//            }
//        }
//
//
//    }
}
