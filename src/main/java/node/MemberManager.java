package node;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.*;

// questa classe serve per le azioni che hanno a che fare con chi
// è responsabile per cosa"

public class MemberManager {
    public static final int N = 4;
    //public static final int W = 3;
    //public static final int R = 2;

    private static final Random rnd = new Random();
    private final int selfId;
    private final ActorRef selfRef;
    private HashMap<Integer, ActorRef> memberList;
    private HashMap<Integer, ActorRef> oldMemberList = new HashMap<>();


    public MemberManager(int selfId, ActorRef selfRef) {
        this.selfId = selfId;
        this.selfRef = selfRef;
        this.memberList = new HashMap<>();
    }

    private ArrayList<ActorRef> obtainResponsibleForData(int key) {
        assert memberList.size() >= N;

        ArrayList<ActorRef> result = new ArrayList<>();
        int remaining = N;

        for (var ref : memberList.entrySet()) {
            if (remaining > 0 && ref.getKey() >= key) {
                remaining--;
                result.add(ref.getValue());
            }
        }

        for (var ref : memberList.entrySet()) {
            if (remaining > 0) {
                remaining--;
                result.add(ref.getValue());
            }
        }

        return result; /////////////////////////////////////////////////////////////////// <- Better use collections.unmodifiablelist(result) ????
    }

    private void multicastToListOfMember(List<ActorRef> dests, Serializable m) {
        // randomly arrange peers
        Collections.shuffle(dests);

        for (ActorRef p : dests)
            sendTo(p, m);
    }

    private void sendTo(ActorRef dest, Serializable m) {
        // simulate network delays using sleep
        try {
            Thread.sleep(rnd.nextInt(10));
        } catch (InterruptedException e) {
            System.err.println("Cannot sleep for some reason");
        }

        // It is allowed sending to himself
        dest.tell(m, this.selfRef);
    }

    public void multicast(Serializable m) {
        multicastToListOfMember(new ArrayList<>(memberList.values()), m);
    }

    public void send(ActorRef destination, Serializable m) {
        sendTo(destination, m);
    }

    public void sendToDataResponsible(int key, Serializable m) {
        multicastToListOfMember(this.obtainResponsibleForData(key), m);
    }

    //Are those the right place? these above methods handle the recovery procedure
    //placed in Node.java
    public void setMemberList(HashMap<Integer, ActorRef> members) {
        this.oldMemberList = this.memberList;
        this.memberList = members;
    }

    public boolean isResponsible(ActorRef node, int key) {
        List<ActorRef> responsible = obtainResponsibleForData(key);
        return responsible.contains(node);
    }

    public void setupTimeoutIn(double ms, int operationId) {
        // TODO
        throw new UnsupportedOperationException();
    }

}
