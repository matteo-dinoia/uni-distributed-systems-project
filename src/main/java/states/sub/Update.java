package states.sub;

import messages.client.DataMsg;
import node.Node;
import node.NodeState;
import states.AbstractState;

public class Update extends AbstractState {
    public Update(Node node, DataMsg.Update msg) {
        super(node);
        handleInitialMsg(msg);
    }

    @Override
    public NodeState getNodeRepresentation() {
        return NodeState.OTHER;
    }

    public void handleInitialMsg(DataMsg.Update msg) {
        // TODO
    }
}
