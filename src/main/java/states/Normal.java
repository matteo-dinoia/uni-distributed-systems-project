package states;

import node.Node;

public class Normal extends AbstractState {
    // Map client to its own controller
    //private final HashMap<ActorRef, Actionator> actionators;

    public Normal(Node node) {
        super(node);
    }
}
