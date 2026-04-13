package server.command;

import common.Message;
import common.MessageType;
import server.ChatRoom;
import server.ChatServer;
import server.ClientHandler;

/**
 * Command Pattern — Concrete Command.
 * Removes a client from their current chat room.
 */
public class LeaveRoomCommand implements Command {

    private final Message msg;
    private final ClientHandler client;
    private final ChatServer server;

    public LeaveRoomCommand(Message msg, ClientHandler client,
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
                                        "You are not in any room.");
            error.stampTimestamp();
            client.sendMessage(error);
            return;
        }

        ChatRoom room = server.getRoomManager().getRoom(roomId);

        if (room != null) {
            room.removeObserver(client);

            // Broadcast leave notification
            Message leaveNotice = new Message(MessageType.SYSTEM_MESSAGE,
                    "SERVER", client.getUsername() + " has left the room.");
            leaveNotice.setRoomId(roomId);
            leaveNotice.stampTimestamp();
            room.notifyObservers(leaveNotice);
        }

        client.setCurrentRoom(null);

        System.out.println("[Server] " + client.getUsername()
                           + " left room: " + roomId);

        Message ack = new Message(MessageType.ACK, "SERVER",
                "You left room '" + roomId + "'.");
        ack.stampTimestamp();
        client.sendMessage(ack);
    }
}