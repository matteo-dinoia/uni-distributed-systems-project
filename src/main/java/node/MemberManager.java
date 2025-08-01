package node;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import messages.Message;
import messages.node_operation.NodeMsg;
import utils.Config;
import utils.Ring;
import utils.Utils;

import java.io.Serializable;
import java.util.*;
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
        this.memberList.put(selfId, this.selfRef);
        this.context = context;
    }

    public void setMemberList(Map<Integer, ActorRef<Message>> members) {
        this.memberList.replaceAll(members);
        if (!members.containsKey(this.selfId))
            this.memberList.put(this.selfId, this.selfRef);
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

    public void sendToAll(Serializable msg) {
        sendTo(memberList.getHashMap().values().stream(), msg);
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
            Thread.sleep(rnd.nextLong(Config.MAX_DELAY.toMillis()));
        } catch (InterruptedException _) {
            System.err.println("Cannot sleep for some reason");
        }

        // It is allowed sending to himself
        Utils.debugPrint("<== NODE " + this.getSelfId() + " SENT TO '" + dest.path().name() + "' " + msg.toString());
        dest.tell(new Message(this.selfRef, msg));
    }

    public void scheduleSendTimeoutToMyself(int operationId) {
        var timeoutMsg = new Message(this.selfRef, new NodeMsg.Timeout(operationId));
        Utils.debugPrint("<~~ NODE " + this.getSelfId() + " scheduled a timeout for operation " + operationId);

        context.getSystem().scheduler().scheduleOnce(
                Config.TIMEOUT,
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
        List<ActorRef<Message>> actors = memberList.getInterval(getSelfId(), Config.N, Config.N);
        ArrayList<ActorRef<Message>> list = new ArrayList<>(actors);
        list.remove(getSelfRef());
        sendTo(list.stream(), msg);
    }

    // DATA RESPONSABILITY

    public List<ActorRef<Message>> getResponsibleForData(int key) {
        Integer firstResponsible = memberList.getCeilKey(key);
        assert firstResponsible != null;
        var res = memberList.getInterval(firstResponsible, 0, Config.N - 1);
        assert res.size() >= Config.N : "Not big enough responsible to send";
        return res;
    }

    /// Used when living find all responsible if there weren't himself
    public List<ActorRef<Message>> findNewResponsiblesFor(int key) {
        Integer firstResponsible = memberList.getFloorKey(key);
        assert firstResponsible != null;

        ArrayList<ActorRef<Message>> list = new ArrayList<>(memberList.getInterval(firstResponsible, 0, Config.N));
        list.remove(getSelfRef());

        if (list.size() < Config.N)
            return null;
        return list;
    }

    // TODO make more efficient
    public boolean willBeResponsible(Integer newNodeId, ActorRef<Message> newNode, Integer key) {
        assert !memberList.getHashMap().containsKey(newNodeId) : "Trying to join an already existing id";
        memberList.put(newNodeId, newNode);
        boolean res = isResponsible(newNode, key);
        memberList.remove(newNodeId);
        return res;
    }

    public boolean isResponsible(ActorRef<Message> actor, int key) {
        return getResponsibleForData(key).contains(actor);
    }

    // COMPUTING DISTANCES

    public int closerLower(int keyA, Integer keyB) {
        if (keyB == null)
            return keyA;

        int distA = memberList.circularDistance(keyA, selfId);
        int distB = memberList.circularDistance(keyB, selfId);
        return distA < distB ? keyA : keyB;
    }

    public int closerHigher(int keyA, Integer keyB) {
        if (keyB == null)
            return keyA;

        int distA = memberList.circularDistance(selfId, keyA);
        int distB = memberList.circularDistance(selfId, keyB);
        return distA < distB ? keyA : keyB;
    }

    public int countMembersBetweenIncluded(int lower, int higher) {
        return memberList.circularDistance(lower, higher);
    }


    public int size() {
        return memberList.size();
    }


}
