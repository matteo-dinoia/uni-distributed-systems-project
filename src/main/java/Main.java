import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import messages.client.StatusMsg;
import node.NodeActor;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;


public class Main {
    //TODO check akka.testing: https://doc.akka.io/libraries/akka-core/current/typed/testing.html

    //TODO check if immutable, and that stuff is correct in our architecture according to akka principles.

    public static void waitUntilEnter() {
        try {
            System.out.println(">>> Press ENTER to exit <<<");
            var ignored = System.in.read();

        } catch (IOException ignored) {
        }
    }

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("project");
        HashMap<Integer, ActorRef> group = new HashMap<>();

        // the first two peers will be participating in a conversation
        group.put(3, system.actorOf(NodeActor.props(3)));
        group.put(10, system.actorOf(NodeActor.props(10)));
        group.put(18, system.actorOf(NodeActor.props(18)));
        group.put(26, system.actorOf(NodeActor.props(26)));

        // send the group member list to everyone in the group
        StatusMsg.InitialMembers initialMsg = new StatusMsg.InitialMembers(1, Collections.unmodifiableMap(group));
        for (ActorRef peer : group.values())
            peer.tell(initialMsg, null);

        // Close
        waitUntilEnter();
        system.terminate();
    }
}
