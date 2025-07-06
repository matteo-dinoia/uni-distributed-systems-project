package node;

import akka.actor.ActorRef;
import messages.ClientMsgs;

import java.io.Serializable;
import java.util.*;

public class MemberManager implements Multicaster {
    public static final int N = 4;
    //public static final int W = 3;
    //public static final int R = 2;

    private static final Random rnd = new Random();
    private final ActorRef selfActionRef;
    private final HashMap<Integer, ActorRef> memberList;

    public MemberManager(ActorRef selfActionRef) {
        this.selfActionRef = selfActionRef;
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

        return result;
    }

    @Override
    public void multicast(Serializable m) {
        multicastToListOfMember(new ArrayList<>(memberList.values()), m);
    }

    @Override
    public void send(ActorRef destination, Serializable m) throws Exception {
        // TODO check error
        sendTo(destination, m);
    }

    @Override
    public void sendToDataResponsible(int key, Serializable m) {
        multicastToListOfMember(this.obtainResponsibleForData(key), m);
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
        dest.tell(m, this.selfActionRef);
    }

    public void handle(ClientMsgs.StatusMsg msg) {
        // TODO implement
    }
}
