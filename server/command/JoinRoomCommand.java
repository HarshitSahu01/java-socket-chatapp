package server.command;

import common.Message;
import common.MessageType;
import server.ChatRoom;
import server.ChatServer;
import server.ClientHandler;

/**
 * Command Pattern — Concrete Command.
 * Joins a client to a chat room.
 */
public class JoinRoomCommand implements Command {

    private final Message msg;
    private final ClientHandler client;
    private final ChatServer server;

    public JoinRoomCommand(Message msg, ClientHandler client,
                           ChatServer server) {
        this.msg = msg;
        this.client = client;
        this.server = server;
    }

    @Override
    public void execute() {
        String roomId = msg.getRoomId();

        if (roomId == null || roomId.trim().isEmpty()) {
            Message error = new Message(MessageType.ERROR, "SERVER",
                                        "Room ID cannot be empty.");
            error.stampTimestamp();
            client.sendMessage(error);
            return;
        }

        roomId = roomId.trim();

        // If client is already in a room, leave it first
        if (client.getCurrentRoom() != null) {
            ChatRoom oldRoom = server.getRoomManager().getRoom(
                                                client.getCurrentRoom());
            if (oldRoom != null) {
                oldRoom.removeObserver(client);
                // Notify old room
                Message leaveNotice = new Message(MessageType.SYSTEM_MESSAGE,
                        "SERVER", client.getUsername() + " has left the room.");
                leaveNotice.setRoomId(oldRoom.getRoomId());
                leaveNotice.stampTimestamp();
                oldRoom.notifyObservers(leaveNotice);
            }
            client.setCurrentRoom(null);
        }

        // Find the room
        ChatRoom room = server.getRoomManager().getRoom(roomId);
        if (room == null) {
            Message error = new Message(MessageType.ERROR, "SERVER",
                    "Room '" + roomId + "' does not exist.");
            error.stampTimestamp();
            client.sendMessage(error);
            return;
        }

        // Check if already in this room
        if (room.hasObserver(client)) {
            Message error = new Message(MessageType.ERROR, "SERVER",
                    "You are already in room '" + roomId + "'.");
            error.stampTimestamp();
            client.sendMessage(error);
            return;
        }

        // Add client as observer
        room.addObserver(client);
        client.setCurrentRoom(roomId);

        System.out.println("[Server] " + client.getUsername()
                           + " joined room: " + roomId);

        // Send ACK to the joining client
        Message ack = new Message(MessageType.ACK, "SERVER",
                "You joined room '" + roomId + "'.");
        ack.setRoomId(roomId);
        ack.stampTimestamp();
        client.sendMessage(ack);

        // Broadcast join notification to room
        Message joinNotice = new Message(MessageType.SYSTEM_MESSAGE, "SERVER",
                client.getUsername() + " has joined the room.");
        joinNotice.setRoomId(roomId);
        joinNotice.stampTimestamp();
        room.notifyObservers(joinNotice);
    }
}