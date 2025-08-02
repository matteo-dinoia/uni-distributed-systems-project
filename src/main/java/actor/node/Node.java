package actor.node;


import actor.node.storage.DataStorage;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import messages.Message;
import messages.node_operation.NodeMsg;
import utils.Config;
import utils.Utils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Node {
    private static final Random rnd = new Random();

    private final MemberManager members;
    private final DataStorage storage;
    private final NodeInfo info;
    private int lastRequestId;

    public Node(int selfId, ActorContext<Message> context, Config config) {
        this.info = new NodeInfo(selfId, context, config);
        this.members = new MemberManager(info);
        this.storage = new DataStorage(info);
        this.lastRequestId = -1;
    }

    // GETTER

    public MemberManager members() {
        return this.members;
    }

    public DataStorage storage() {
        return this.storage;
    }

    public Config config() {
        return info.config();
    }

    public int getFreshRequestId() {
        return ++this.lastRequestId;
    }

    public int id() {
        return info.id();
    }

    public ActorRef<Message> actorRef() {
        return info.self();
    }

    // SENDER

    public void sendToAll(Serializable msg) {
        sendTo(members.getMembers().values().stream(), msg);
    }

    public void sendTo(Stream<ActorRef<Message>> dest, Serializable msg) {
        // randomly arrange peers
        var dests = dest.collect(Collectors.toList());
        Collections.shuffle(dests);

        for (ActorRef<Message> p : dests)
            sendTo(p, msg);
    }

    public void sendTo(ActorRef<Message> dest, Serializable msg) {
        // simulate network delays using sleep
        try {
            Thread.sleep(rnd.nextLong(info.config().MAX_DELAY().toMillis()));
        } catch (InterruptedException _) {
        }

        // It is allowed sending to himself
        Utils.debugPrint(info.config().DEBUG(),
                "<== NODE " + info.id() + " SENT TO '" + dest.path().name() + "' " + msg.toString());
        dest.tell(new Message(info.self(), msg));
    }

    public void sendToResponsible(int key, Serializable msg) {
        sendTo(members.getResponsibles(key).stream(), msg);
    }

    public void scheduleTimeout(int operationId) {
        var timeoutMsg = new Message(info.self(), new NodeMsg.Timeout(operationId));
        Utils.debugPrint(info.config().DEBUG(),
                "<~~ NODE " + info.id() + " scheduled a timeout for operation " + operationId);

        info.context().getSystem().scheduler().scheduleOnce(
                info.config().TIMEOUT(),
                () -> info.self().tell(timeoutMsg),
                info.context().getExecutionContext()
        );
    }
}
