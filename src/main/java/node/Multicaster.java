package node;

import akka.actor.ActorRef;

import java.io.Serializable;

// TODO change name
public interface Multicaster {
    void multicast(Serializable m);

    void send(ActorRef destination, Serializable m) throws Exception;

    void sendToDataResponsible(int key, Serializable m);
}
