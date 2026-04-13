package server.command;

import common.Message;
import common.MessageType;
import server.ChatRoom;
import server.ChatServer;
import server.ClientHandler;

/**
 * Command Pattern — Concrete Command.
 * Broadcasts a chat message to all observers in the room.
 */
public class SendMessageCommand implements Command {

    private final Message msg;
    private final ClientHandler client;
    private final ChatServer server;

    public SendMessageCommand(Message msg, ClientHandler client,
                              ChatServer server) {
        this.msg = msg;
        this.client = client;
        this.server = server;
    }

    @Override
    public void execute() {
        String roomId = client.getCurrentRoom();

        if (roomId == null) {
            Message error = new Message(MessageType.ERROR, "SERVER",
                    "You are not in any room. Join a room first.");
            error.stampTimestamp();
            client.sendMessage(error);
            return;
        }

        ChatRoom room = server.getRoomManager().getRoom(roomId);
        if (room == null) {
            Message error = new Message(MessageType.ERROR, "SERVER",
                    "Room no longer exists.");
            error.stampTimestamp();
            client.sendMessage(error);
            client.setCurrentRoom(null);
            return;
        }

        // Build the broadcast message — timestamp set on server
        Message broadcast = new Message(MessageType.CHAT_MESSAGE,
                client.getUsername(), msg.getContent());
        broadcast.setRoomId(roomId);
        broadcast.stampTimestamp(); // Server stamps the time

        // Notify all observers in the room
        room.notifyObservers(broadcast);
    }
}