package node;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import messages.node_operation.NodeMsg;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// questa classe serve per le azioni che hanno a che fare con chi
// è responsabile per cosa"

public class MemberManager {
    private static final Random rnd = new Random();
    private final int selfId;
    private final ActorRef selfRef;
    private final ActorContext context;
    private HashMap<Integer, ActorRef> memberList;


    public MemberManager(int selfId, ActorRef selfRef, ActorContext context) {
        this.selfId = selfId;
        this.selfRef = selfRef;
        this.memberList = new HashMap<>();
        this.context = context;
    }

    private void sendToListOfMember(Stream<ActorRef> dest, Serializable m) {
        // randomly arrange peers
        var dests = dest.collect(Collectors.toList());
        Collections.shuffle(dests);

        for (ActorRef p : dests)
            sendTo(p, m);
    }


    public void sendTo(ActorRef dest, Serializable m) {
        // simulate network delays using sleep
        try {
            Thread.sleep(rnd.nextInt(10));
        } catch (InterruptedException e) {
            System.err.println("Cannot sleep for some reason");
        }

        // It is allowed sending to himself
        dest.tell(m, this.selfRef);
    }

    public void sendTo(Predicate<Integer> filter, Serializable m) {
        Stream<ActorRef> dests = this.memberList.entrySet().stream().filter(el -> filter.test(el.getKey())).map(Map.Entry::getValue);
        sendToListOfMember(dests, m);
    }

    public void sendToAll(Serializable m) {
        sendToListOfMember(memberList.values().stream(), m);
    }

    public void sendToDataResponsible(int key, Serializable m) {
        sendTo(actor_id -> {
            return true;
        }, m);
    }

    public void scheduleSendTimeoutToMyself(int operationId) {
        context.system().scheduler().scheduleOnce(
                Duration.create(1000, TimeUnit.MILLISECONDS),  // how frequently generate them
                this.selfRef,                                        // destination actor reference
                new NodeMsg.Timeout(operationId),                    // the message to send
                context.system().dispatcher(),                       // system dispatcher
                this.selfRef);                                       // source of the message (myself)
    }

    public void sendTo2n(Serializable msg) {
        // TODO
        throw new UnsupportedOperationException();
    }


    public void setMemberList(HashMap<Integer, ActorRef> members) {
        this.memberList = members;
    }

    public HashMap<Integer, ActorRef> getMemberList() {
        return this.memberList;
    }

    public ActorRef getSelfRef() {
        return selfRef;
    }

    public int getSelfId() {
        return selfId;
    }

    public boolean isResponsible(ActorRef requester, Integer key) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public int circularDistance(int from, int to) {
        int size = memberList.size();
        return (to - from + size) % size;
    }

    public boolean isCloserClockwise(int candidate, int current, int self) {
        int size = memberList.size();
        return circularDistance(self, candidate) > circularDistance(self, current);
    }

    public boolean isCloserCounterClockwise(int candidate, int current, int self) {
        int size = memberList.size();
        return circularDistance(candidate, self) > circularDistance(current, self);
    }

    //TODO
    public int countMembersBetweenIncluded(int closestHigherResponded, int closestLowerResponded) {
        //count the entries between the two higher and lower responses.

        Integer distance = null;


        return distance;
    }

}
