package de.reondo.akka.remoting.messages;

import akka.actor.ActorRef;

import java.io.Serializable;

public class AnnounceRun implements Serializable {
    private ActorRef sender;
    private int numWarmupMessages;
    private int numMessages;

    public AnnounceRun(ActorRef sender, int numWarmupMessages, int numMessages) {
        this.sender = sender;
        this.numWarmupMessages = numWarmupMessages;
        this.numMessages = numMessages;
    }

    public ActorRef getSender() {
        return sender;
    }

    public int getNumMessages() {
        return numMessages;
    }

    public int getNumWarmupMessages() {
        return numWarmupMessages;
    }

    public int getTotalNumberOfMessages() {
        return numMessages + numWarmupMessages;
    }
}
