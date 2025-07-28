package node;


import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import messages.Message;
import messages.node_operation.NodeMsg;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// questa classe serve per le azioni che hanno a che fare con chi
// è responsabile per cosa"

public class MemberManager {
    private static final Random rnd = new Random();
    private final int selfId;
    private final ActorRef<Message> selfRef;
    private final ActorContext<Message> context;
    private HashMap<Integer, ActorRef<Message>> memberList;


    public MemberManager(int selfId, ActorRef<Message> selfRef, ActorContext<Message> context) {
        this.selfId = selfId;
        this.selfRef = selfRef;
        this.memberList = new HashMap<>();
        this.context = context;
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

    public void sendTo(Predicate<Integer> filter, Serializable m) {
        Stream<ActorRef<Message>> dests = this.memberList.entrySet().stream().filter(el -> filter.test(el.getKey())).map(Map.Entry::getValue);
        sendTo(dests, m);
    }

    public void sendToAll(Serializable m) {
        sendTo(memberList.values().stream(), m);
    }

    public void scheduleSendTimeoutToMyself(int operationId) {
        var timeoutMsg = new Message(this.selfRef, new NodeMsg.Timeout(operationId));

        context.getSystem().scheduler().scheduleOnce(
                Duration.create(1000, TimeUnit.MILLISECONDS),
                () -> this.selfRef.tell(timeoutMsg),
                context.getExecutionContext()
        );
    }

    public void sendToDataResponsible(int key, Serializable m) {
        // TODO HARD implement
        throw new UnsupportedOperationException();
    }

    public void sendTo2n(Serializable msg) {
        // TODO HARD implement
        throw new UnsupportedOperationException();
    }


    public void setMemberList(HashMap<Integer, ActorRef<Message>> members) {
        this.memberList = members;
    }

    public HashMap<Integer, ActorRef<Message>> getMemberList() {
        return this.memberList;
    }

    public ActorRef<Message> getSelfRef() {
        return selfRef;
    }

    public int getSelfId() {
        return selfId;
    }

    public boolean isResponsible(ActorRef<Message> requester, Integer key) {
        // TODO HARD
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

    public int countMembersBetweenIncluded(int closestHigherResponded, int closestLowerResponded) {
        // TODO HARD count the entries between the two higher and lower responses.
        throw new UnsupportedOperationException("Not implemented yet");
    }


    public List<ActorRef<Message>> findNewResponsiblesFor(int key) {
        // TODO HARD
        throw new UnsupportedOperationException("Not implemented yet");
    }


}
