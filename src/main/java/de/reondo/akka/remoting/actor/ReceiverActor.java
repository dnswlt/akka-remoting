package de.reondo.akka.remoting.actor;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.ExtendedActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.remote.WireFormats;
import akka.remote.serialization.ProtobufSerializer;
import de.reondo.akka.remoting.proto.Messages;

import java.time.Duration;

public class ReceiverActor extends AbstractActorWithTimers {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final int backpressureValue;
    private long firstMessageReceived;
    private long lastMessageReceived;
    private int numMessagesReceived;
    private int numWarmupMessages;
    private int remainingRequestedMessages;
    private ActorRef sender;

    public static Props props(int backpressureValue) {
        return Props.create(ReceiverActor.class, backpressureValue);
    }

    public ReceiverActor(int backpressureValue) {
        this.backpressureValue = backpressureValue;
        getTimers().startPeriodicTimer("TickKey", "Tick", Duration.ofMillis(1000));
        log.info("Receiver starts with backpressureValue={}", backpressureValue);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Messages.BinaryMessage.class, this::onBinaryMessage)
            .match(Messages.AnnounceRun.class, this::onAnnounceRun)
            .matchEquals("Tick", t -> logStatus())
            .build();
    }

    private ActorRef deserializeActorRef(String serializedActorRef) {
        WireFormats.ActorRefData actorRefData = WireFormats.ActorRefData.newBuilder().setPath(serializedActorRef).build();
        return ProtobufSerializer.deserializeActorRef((ExtendedActorSystem) getContext().getSystem(), actorRefData);
    }

    private void onAnnounceRun(Messages.AnnounceRun announceRun) {
        numMessagesReceived = 0;
        firstMessageReceived = 0;
        lastMessageReceived = 0;
        sender = deserializeActorRef(announceRun.getSerializedActorRef());
        numWarmupMessages = announceRun.getNumWarmupMessages();
        remainingRequestedMessages = backpressureValue;
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
                log.info("Warmup finished. Run begins");
            }
            lastMessageReceived = now;
            numMessagesReceived++;
        }
        remainingRequestedMessages--;
        if (remainingRequestedMessages < 100) {
            sender.tell(Messages.Backpressure.newBuilder().setNumAcceptedMessages(backpressureValue).build(), getSelf());
            remainingRequestedMessages += backpressureValue;
        }
    }
}
