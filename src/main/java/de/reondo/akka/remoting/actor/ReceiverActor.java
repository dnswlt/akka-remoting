package de.reondo.akka.remoting.actor;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import de.reondo.akka.remoting.messages.AnnounceRun;
import de.reondo.akka.remoting.proto.Messages;

import java.time.Duration;

public class ReceiverActor extends AbstractActorWithTimers {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private static final int BACKPRESSURE_VALUE = 1000;

    private long firstMessageReceived;
    private long lastMessageReceived;
    private int numMessagesReceived;
    private int numWarmupMessages;
    private int remainingRequestedMessages;
    private ActorRef sender;

    public static Props props() {
        return Props.create(ReceiverActor.class);
    }

    public ReceiverActor() {
        getTimers().startPeriodicTimer("TickKey", "Tick", Duration.ofMillis(1000));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Messages.BinaryMessage.class, this::onBinaryMessage)
            .match(AnnounceRun.class, this::onAnnounceRun)
            .matchEquals("Tick", t -> logStatus())
            .build();
    }

    private void onAnnounceRun(AnnounceRun announceRun) {
        numMessagesReceived = 0;
        firstMessageReceived = 0;
        lastMessageReceived = 0;
        sender = announceRun.getSender();
        numWarmupMessages = announceRun.getNumWarmupMessages();
        remainingRequestedMessages = BACKPRESSURE_VALUE;
        sender.tell(Messages.Backpressure.newBuilder()
            .setNumAcceptedMessages(remainingRequestedMessages)
            .build(), getSelf());
    }

    private void logStatus() {
        log.info("Received {} messages in {}ms ({}msg/s)", numMessagesReceived,
            lastMessageReceived - firstMessageReceived,
            numMessagesReceived * 1000.0 / (lastMessageReceived - firstMessageReceived)
        );
    }

    private void onBinaryMessage(Messages.BinaryMessage message) {
        long now = System.currentTimeMillis();
        if (numWarmupMessages > 0) {
            numWarmupMessages--;
        } else {
            if (numMessagesReceived == 0) {
                firstMessageReceived = now;
            }
            lastMessageReceived = now;
            numMessagesReceived++;
        }
        remainingRequestedMessages--;
        if (remainingRequestedMessages < 100) {
            sender.tell(Messages.Backpressure.newBuilder().setNumAcceptedMessages(BACKPRESSURE_VALUE).build(), getSelf());
            remainingRequestedMessages += BACKPRESSURE_VALUE;
        }
    }
}
