package server.command;

import common.Message;
import common.MessageType;
import server.ChatServer;
import server.ChatRoom;
import server.ClientHandler;

/**
 * Command Pattern — Concrete Command.
 * Creates a new chat room.
 */
public class CreateRoomCommand implements Command {

    private final Message msg;
    private final ClientHandler client;
    private final ChatServer server;

    public CreateRoomCommand(Message msg, ClientHandler client,
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
        ChatRoom room = server.getRoomManager().createRoom(roomId,
                                                    client.getUsername());

        if (room == null) {
            Message error = new Message(MessageType.ERROR, "SERVER",
                    "Room '" + roomId + "' already exists.");
            error.stampTimestamp();
            client.sendMessage(error);
            return;
        }

        System.out.println("[Server] Room created: " + roomId
                           + " by " + client.getUsername());

        Message ack = new Message(MessageType.ACK, "SERVER",
                "Room '" + roomId + "' created successfully.");
        ack.stampTimestamp();
        client.sendMessage(ack);
    }
}