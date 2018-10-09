package de.reondo.akka.remoting.actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.remote.WireFormats;
import akka.remote.serialization.ProtobufSerializer;
import com.google.protobuf.ByteString;
import de.reondo.akka.remoting.proto.Messages;

import java.time.ZonedDateTime;
import java.util.UUID;

public class SenderActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private ActorRef receiver;
    private int numMesssages;
    private int remainingMessages;

    public static Props props() {
        return Props.create(SenderActor.class);
    }

    public static class Send {
        public final int numMesssages;
        public final ActorPath actorPath;

        public Send(ActorPath actorPath, int numMesssages) {
            this.numMesssages = numMesssages;
            this.actorPath = actorPath;
        }
    }

    public SenderActor() {
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Send.class, this::onSend)
            .match(ActorIdentity.class, this::onActorIdentity)
            .match(Messages.Backpressure.class, this::onBackpressure)
            .build();
    }

    private void onBackpressure(Messages.Backpressure backpressure) {
        byte[] payload = new byte[512];
        ZonedDateTime now = ZonedDateTime.now();
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) i;
        }
        long before = System.currentTimeMillis();
        UUID messageId = UUID.randomUUID();
        int remaining = Math.min(remainingMessages, backpressure.getNumAcceptedMessages());
        int sent = 0;
        Messages.BinaryMessage message = Messages.BinaryMessage.newBuilder()
            .setId(messageId.toString())
            .setTimestamp(now.toInstant().toEpochMilli())
            .setPayload(ByteString.copyFrom(payload))
            .build();
        while (sent < remaining) {
            receiver.tell(message, getSelf());
            sent++;
        }
        remainingMessages -= sent;
        long duration = System.currentTimeMillis() - before;
        log.info("Sent {} messages to {} in {}ms", sent, receiver, duration);
    }

    private void onActorIdentity(ActorIdentity identity) {
        if (!identity.getActorRef().isPresent()) {
            log.info("Actor could not be identified :(");
            return;
        }
        receiver = identity.getActorRef().get();
        int numWarmupMessages = (int) (numMesssages * 0.1);
        remainingMessages = numMesssages + numWarmupMessages;
        WireFormats.ActorRefData serializedActorRef = ProtobufSerializer.serializeActorRef(getSelf());
        Messages.AnnounceRun announceRun = Messages.AnnounceRun.newBuilder()
            .setNumMessages(numMesssages)
            .setNumWarmupMessages(numWarmupMessages)
            .setSerializedActorRef(serializedActorRef.getPath())
            .build();
        receiver.tell(announceRun, getSelf());
    }

    private void onSend(Send send) {
        numMesssages = send.numMesssages;
        ActorSelection selection = getContext().actorSelection(send.actorPath);
        selection.tell(new Identify(UUID.randomUUID().toString()), getSelf());
    }
}
