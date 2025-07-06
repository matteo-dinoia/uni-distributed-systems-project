package node;

import akka.actor.ActorRef;

import java.io.Serializable;

// TODO change name
@SuppressWarnings("unused")
public interface Multicaster {
    void multicast(Serializable m);

    void send(ActorRef destination, Serializable m);

    void sendToDataResponsible(int key, Serializable m);
}
