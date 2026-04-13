package server.command;

import common.Message;
import common.MessageType;
import server.ChatServer;
import server.ClientHandler;

/**
 * Command Pattern — Concrete Command.
 * Handles client disconnection.
 */
public class DisconnectCommand implements Command {

    private final ClientHandler client;
    private final ChatServer server;

    public DisconnectCommand(ClientHandler client, ChatServer server) {
        this.client = client;
        this.server = server;
    }

    @Override
    public void execute() {
        String username = client.getUsername();
        System.out.println("[Server] User disconnecting: " + username);

        // Remove from all rooms
        server.getRoomManager().removeClientFromAllRooms(client);

        // Unregister from server
        server.removeClient(username);

        // Send ACK before closing
        Message ack = new Message(MessageType.ACK, "SERVER",
                                  "Goodbye, " + username + "!");
        ack.stampTimestamp();
        client.sendMessage(ack);

        // Close the connection
        client.disconnect();
    }
}