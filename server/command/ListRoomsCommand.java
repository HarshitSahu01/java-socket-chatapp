package server.command;

import common.Message;
import common.MessageType;
import server.ChatServer;
import server.ClientHandler;

/**
 * Command Pattern — Concrete Command.
 * Lists all available chat rooms.
 */
public class ListRoomsCommand implements Command {

    private final ClientHandler client;
    private final ChatServer server;

    public ListRoomsCommand(ClientHandler client, ChatServer server) {
        this.client = client;
        this.server = server;
    }

    @Override
    public void execute() {
        String roomList = server.getRoomManager().listRooms();

        Message response = new Message(MessageType.ROOM_LIST, "SERVER", roomList);
        response.stampTimestamp();
        client.sendMessage(response);
    }
}