package common;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String secret;
    private String sender;
    private String roomId;
    private String content;
    private long timestamp;

    public Message() {}

    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
    }

    public Message(MessageType type, String secret, String sender, String roomId, String content) {
        this.type = type;
        this.secret = secret;
        this.sender = sender;
        this.roomId = roomId;
        this.content = content;
    }

    public MessageType getType()       { return type; }
    public String getSecret()          { return secret; }
    public String getSender()          { return sender; }
    public String getRoomId()          { return roomId; }
    public String getContent()         { return content; }
    public long getTimestamp()         { return timestamp; }

    public void setType(MessageType type)      { this.type = type; }
    public void setSecret(String secret)       { this.secret = secret; }
    public void setSender(String sender)       { this.sender = sender; }
    public void setRoomId(String roomId)       { this.roomId = roomId; }
    public void setContent(String content)     { this.content = content; }
    public void setTimestamp(long timestamp)   { this.timestamp = timestamp; }

    public void stampTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "Message{type=" + type + ", sender=" + sender + ", room=" + roomId + ", content=" + content + "}";
    }
}