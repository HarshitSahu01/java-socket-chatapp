package server;

import common.Message;
import common.MessageType;

import java.util.concurrent.ConcurrentHashMap;

public class ChatRoomManager {

    private final ConcurrentHashMap<String, ChatRoom> rooms;

    public ChatRoomManager() {
        this.rooms = new ConcurrentHashMap<>();
    }

    public ChatRoom createRoom(String roomId, String creator) {
        ChatRoom room = new ChatRoom(roomId, creator);
        ChatRoom existing = rooms.putIfAbsent(roomId, room);
        return existing == null ? room : null;
    }

    public boolean deleteRoom(String roomId, String requester) {
        ChatRoom room = rooms.get(roomId);
        if (room == null || !room.getCreator().equals(requester)) return false;
        room.notifyAndClear("Room '" + roomId + "' has been deleted by the creator.");
        rooms.remove(roomId);
        return true;
    }

    public ChatRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public String listRooms() {
        if (rooms.isEmpty()) return "No rooms available. Create one!";
        StringBuilder sb = new StringBuilder("Available Rooms:\n");
        for (ChatRoom room : rooms.values()) {
            sb.append("  - ").append(room.getRoomId())
              .append(" (Creator: ").append(room.getCreator())
              .append(", Members: ").append(room.getObserverCount()).append(")\n");
        }
        return sb.toString().trim();
    }

    public void removeClientFromAllRooms(ClientHandler client) {
        for (ChatRoom room : rooms.values()) {
            if (room.hasObserver(client)) {
                room.removeObserver(client);
                Message notice = new Message(MessageType.SYSTEM_MESSAGE, "SERVER",
                        client.getUsername() + " has disconnected.");
                notice.setRoomId(room.getRoomId());
                notice.stampTimestamp();
                room.notifyObservers(notice);
            }
        }
    }
}