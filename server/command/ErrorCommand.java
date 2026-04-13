package server.command;

import common.Message;
import common.MessageType;
import server.ClientHandler;

/**
 * Fallback command for unknown or invalid message types.
 */
public class ErrorCommand implements Command {

    private final ClientHandler client;
    private final String errorMessage;

    public ErrorCommand(ClientHandler client, String errorMessage) {
        this.client = client;
        this.errorMessage = errorMessage;
    }

    @Override
    public void execute() {
        Message response = new Message(MessageType.ERROR, "SERVER", errorMessage);
        response.stampTimestamp();
        client.sendMessage(response);
    }
}