package server.command;

import common.Message;
import common.MessageType;
import server.ChatServer;
import server.ClientHandler;

/**
 * Factory Pattern — Creates the correct Command object
 * based on the message type.
 */
public class CommandFactory {

    // Private constructor — static factory
    private CommandFactory() {
    }

    /**
     * Create a Command from the incoming message.
     *
     * @param msg    The message received from the client.
     * @param client The ClientHandler that sent the message.
     * @param server The ChatServer reference.
     * @return A Command ready to execute.
     */
    public static Command create(Message msg, ClientHandler client,
                                 ChatServer server) {
        if (msg == null || msg.getType() == null) {
            return new ErrorCommand(client, "Invalid message received.");
        }

        switch (msg.getType()) {
            case CONNECT:
                return new ConnectCommand(msg, client, server);
            case DISCONNECT:
                return new DisconnectCommand(client, server);
            case LIST_ROOMS:
                return new ListRoomsCommand(client, server);
            case CREATE_ROOM:
                return new CreateRoomCommand(msg, client, server);
            case DELETE_ROOM:
                return new DeleteRoomCommand(msg, client, server);
            case JOIN_ROOM:
                return new JoinRoomCommand(msg, client, server);
            case LEAVE_ROOM:
                return new LeaveRoomCommand(msg, client, server);
            case CHAT_MESSAGE:
                return new SendMessageCommand(msg, client, server);
            default:
                return new ErrorCommand(client, "Unknown command type: "
                                        + msg.getType());
        }
    }
}