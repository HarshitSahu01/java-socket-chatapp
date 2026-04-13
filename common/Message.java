package common;

import java.io.Serializable;

/**
 * Unified message object for all communication.
 * Serializable so it can be sent over ObjectStreams.
 * Timestamp is set on the server side only.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String secret;
    private String sender;
    private String roomId;
    private String content;
    private long timestamp; // Set by server only

    // Default constructor
    public Message() {
    }

    // Constructor for convenience
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
    }

    // Full constructor
    public Message(MessageType type, String secret, String sender,
                   String roomId, String content) {
        this.type = type;
        this.secret = secret;
        this.sender = sender;
        this.roomId = roomId;
        this.content = content;
    }

    // --- Getters ---

    public MessageType getType() {
        return type;
    }

    public String getSecret() {
        return secret;
    }

    public String getSender() {
        return sender;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // --- Setters ---

    public void setType(MessageType type) {
        this.type = type;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Stamp the current server time onto this message.
     */
    public void stampTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "Message{type=" + type + ", sender=" + sender +
               ", room=" + roomId + ", content=" + content + "}";
    }
}