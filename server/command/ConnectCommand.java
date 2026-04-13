package server.command;

import common.Message;
import common.MessageType;
import server.ChatServer;
import server.ClientHandler;

/**
 * Command Pattern — Concrete Command.
 * Handles a new client connection (username registration).
 */
public class ConnectCommand implements Command {

    private final Message msg;
    private final ClientHandler client;
    private final ChatServer server;

    public ConnectCommand(Message msg, ClientHandler client,
                          ChatServer server) {
        this.msg = msg;
        this.client = client;
        this.server = server;
    }

    @Override
    public void execute() {
        String username = msg.getSender();

        // Validate username
        if (username == null || username.trim().isEmpty()) {
            Message error = new Message(MessageType.ERROR, "SERVER",
                                        "Username cannot be empty.");
            error.stampTimestamp();
            client.sendMessage(error);
            return;
        }

        username = username.trim();

        // Check if username already taken
        if (server.isUsernameTaken(username)) {
            Message error = new Message(MessageType.ERROR, "SERVER",
                    "Username '" + username + "' is already taken.");
            error.stampTimestamp();
            client.sendMessage(error);
            return;
        }

        // Register the client
        client.setUsername(username);
        server.registerClient(username, client);

        System.out.println("[Server] User connected: " + username);

        // Send ACK
        Message ack = new Message(MessageType.ACK, "SERVER",
                "Welcome, " + username + "! You are now connected.");
        ack.stampTimestamp();
        client.sendMessage(ack);
    }
}