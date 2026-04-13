package server.command;

import common.Message;
import common.MessageType;
import server.ChatServer;
import server.ClientHandler;

/**
 * Command Pattern — Concrete Command.
 * Deletes a chat room (only the creator can delete).
 */
public class DeleteRoomCommand implements Command {

    private final Message msg;
    private final ClientHandler client;
    private final ChatServer server;

    public DeleteRoomCommand(Message msg, ClientHandler client,
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
        boolean deleted = server.getRoomManager().deleteRoom(roomId,
                                                    client.getUsername());

        if (!deleted) {
            Message error = new Message(MessageType.ERROR, "SERVER",
                    "Cannot delete room '" + roomId
                    + "'. Either it doesn't exist or you are not the creator.");
            error.stampTimestamp();
            client.sendMessage(error);
            return;
        }

        System.out.println("[Server] Room deleted: " + roomId
                           + " by " + client.getUsername());

        Message ack = new Message(MessageType.ACK, "SERVER",
                "Room '" + roomId + "' deleted successfully.");
        ack.stampTimestamp();
        client.sendMessage(ack);
    }
}