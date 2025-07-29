package node;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import messages.Message;
import messages.node_operation.NodeMsg;
import scala.concurrent.duration.Duration;
import utils.Config;
import utils.Ring;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MemberManager {
    private static final Random rnd = new Random();
    private final int selfId;
    private final ActorRef<Message> selfRef;
    private final ActorContext<Message> context;
    private final Ring<ActorRef<Message>> memberList;


    public MemberManager(int selfId, ActorContext<Message> context) {
        this.selfId = selfId;
        this.selfRef = context.getSelf();
        this.memberList = new Ring<>();
        this.context = context;
    }

    public void setMemberList(HashMap<Integer, ActorRef<Message>> members) {
        this.memberList.replaceAll(members);
    }

    public void addMember(int key, ActorRef<Message> member) {
        this.memberList.put(key, member);
    }

    public void removeMember(int key) {
        this.memberList.remove(key);
    }

    public HashMap<Integer, ActorRef<Message>> getMemberList() {
        return this.memberList.getHashMap();
    }

    public ActorRef<Message> getSelfRef() {
        return selfRef;
    }

    public int getSelfId() {
        return selfId;
    }

    // SENDERS

    public void sendToAll(Serializable m) {
        sendTo(memberList.getHashMap().values().stream(), m);
    }

    public void sendTo(Stream<ActorRef<Message>> dest, Serializable m) {
        // randomly arrange peers
        var dests = dest.collect(Collectors.toList());
        Collections.shuffle(dests);

        for (ActorRef<Message> p : dests)
            sendTo(p, m);
    }

    public void sendTo(ActorRef<Message> dest, Serializable m) {
        // simulate network delays using sleep
        try {
            Thread.sleep(rnd.nextInt(10));
        } catch (InterruptedException e) {
            System.err.println("Cannot sleep for some reason");
        }

        // It is allowed sending to himself
        dest.tell(new Message(this.selfRef, m));
    }

    public void scheduleSendTimeoutToMyself(int operationId) {
        var timeoutMsg = new Message(this.selfRef, new NodeMsg.Timeout(operationId));

        context.getSystem().scheduler().scheduleOnce(
                Duration.create(1000, TimeUnit.MILLISECONDS),
                () -> this.selfRef.tell(timeoutMsg),
                context.getExecutionContext()
        );
    }

    /// Return the amount of messages actually sent
    /// This is needed as it is needed to know the amount of ack to wait
    public void sendToDataResponsible(int key, Serializable msg) {
        sendTo(getResponsibleForData(key).stream(), msg);
    }

    public void sendTo2n(Serializable msg) {
        Integer key = memberList.getFloorKey(this.selfId);
        assert key != null;

        List<ActorRef<Message>> actors = memberList.getInterval(key, Config.N, Config.N);
        sendTo(actors.stream(), msg);
    }

    // DATA RESPONSABILITY

    private List<ActorRef<Message>> getResponsibleForData(int key) {
        Integer firstResponsible = memberList.getFloorKey(key);
        assert firstResponsible != null;

        return memberList.getInterval(firstResponsible, 0, Config.N);

    }

    /// Used when living find all responsible if there weren't himself
    public List<ActorRef<Message>> findNewResponsiblesFor(int key) {
        Integer firstResponsible = memberList.getFloorKey(key);
        assert firstResponsible != null;

        List<ActorRef<Message>> list = memberList.getInterval(firstResponsible, 0, Config.N + 1);
        list.remove(getSelfId());

        if (list.size() < Config.N)
            return null;
        return list;
    }

    public boolean isResponsible(ActorRef<Message> actor, int key) {
        return getResponsibleForData(key).contains(actor);
    }

    // COMPUTING DISTANCES


    public Integer closerLower(Integer keyA, Integer keyB) {
        if (keyA == null)
            return keyB;
        if (keyB == null)
            return keyA;

        if (memberList.circularDistance(keyA, selfId) < memberList.circularDistance(keyB, selfId))
            return keyA;
        else
            return keyB;
    }

    public Integer closerHigher(Integer keyA, Integer keyB) {
        if (keyA == null)
            return keyB;
        if (keyB == null)
            return keyA;

        if (memberList.circularDistance(selfId, keyA) < memberList.circularDistance(selfId, keyB))
            return keyA;
        else
            return keyB;
    }

    public int countMembersBetweenIncluded(int lower, int higher) {
        return memberList.circularDistance(lower, higher);
    }


}
