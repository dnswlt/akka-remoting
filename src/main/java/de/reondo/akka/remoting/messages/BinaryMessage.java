package de.reondo.akka.remoting.messages;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

public class BinaryMessage implements Serializable {
    private UUID messageId;
    private ZonedDateTime timestamp;
    private byte[] payload;

    public BinaryMessage(UUID messageId, ZonedDateTime timestamp, byte[] payload) {
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public byte[] getPayload() {
        return payload;
    }

}
