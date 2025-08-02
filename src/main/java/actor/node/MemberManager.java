package actor.node;

import akka.actor.typed.ActorRef;
import messages.Message;
import utils.Config;
import utils.structs.Ring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MemberManager {
    private final NodeInfo node;
    private final Ring<ActorRef<Message>> memberList;


    public MemberManager(NodeInfo nodeInfo) {
        this.node = nodeInfo;
        this.memberList = new Ring<>();
        this.memberList.put(nodeInfo.id(), nodeInfo.self());
    }

    public void setMembers(Map<Integer, ActorRef<Message>> members) {
        this.memberList.replaceAll(members);
        this.memberList.put(node.id(), node.self());
    }

    public void addMember(int key, ActorRef<Message> member) {
        this.memberList.put(key, member);
    }

    public void removeMember(int key) {
        this.memberList.remove(key);
    }

    // Get Lists

    public Map<Integer, ActorRef<Message>> getMembers() {
        return this.memberList.getMap();
    }

    public ArrayList<ActorRef<Message>> getNodeToCommunicateForJoin() {
        var list = new ArrayList<>(memberList.getInterval(node.id(), Config.N - 1, Config.N - 1));
        list.remove(node.self());
        return list;
    }

    // DATA RESPONSABILITY

    public List<ActorRef<Message>> getResponsibles(int key) {
        Integer firstResponsible = memberList.getCeilKey(key);
        assert firstResponsible != null;
        var res = memberList.getInterval(firstResponsible, 0, Config.N - 1);
        assert res.size() >= Config.N : "Not big enough responsible to send";
        return res;
    }

    /// Used when living find all responsible after this node leaves
    public List<ActorRef<Message>> findNewResponsibles(int key) {
        Integer firstResponsible = memberList.getFloorKey(key);
        assert firstResponsible != null;

        ArrayList<ActorRef<Message>> list = new ArrayList<>(memberList.getInterval(firstResponsible, 0, Config.N));
        list.remove(node.self());

        assert list.size() >= Config.N : "List of responsible is smaller than N";
        return list;
    }

    public boolean willBeResponsible(Integer newNodeId, ActorRef<Message> newNode, Integer key) {
        assert !memberList.getMap().containsKey(newNodeId) : "Trying to join an already existing id";
        memberList.put(newNodeId, newNode);
        boolean res = isResponsible(newNode, key);
        memberList.remove(newNodeId);
        return res;
    }

    public boolean isResponsible(ActorRef<Message> actor, int key) {
        return getResponsibles(key).contains(actor);
    }

    // OTHERS

    public int size() {
        return memberList.size();
    }
}
