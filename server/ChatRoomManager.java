package server;

import server.observer.ChatRoomObserver;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all chat rooms on the server.
 * Thread-safe via ConcurrentHashMap.
 */
public class ChatRoomManager {

    private final ConcurrentHashMap<String, ChatRoom> rooms;

    public ChatRoomManager() {
        this.rooms = new ConcurrentHashMap<>();
    }

    /**
     * Create a new room. Returns the room if created, null if ID taken.
     */
    public ChatRoom createRoom(String roomId, String creator) {
        if (rooms.containsKey(roomId)) {
            return null;
        }
        ChatRoom room = new ChatRoom(roomId, creator);
        ChatRoom existing = rooms.putIfAbsent(roomId, room);
        if (existing != null) {
            return null; // Race condition — another thread created it first
        }
        return room;
    }

    /**
     * Delete a room. Only the creator can delete it.
     * Returns true if deleted, false otherwise.
     */
    public boolean deleteRoom(String roomId, String requester) {
        ChatRoom room = rooms.get(roomId);
        if (room == null) {
            return false;
        }
        if (!room.getCreator().equals(requester)) {
            return false;
        }

        // Notify all observers that room is being deleted
        room.notifyAndClear("Room '" + roomId
                            + "' has been deleted by the creator.");

        // Reset currentRoom for all observers that were in this room
        // (handled by client when they receive the SYSTEM_MESSAGE)

        rooms.remove(roomId);
        return true;
    }

    /**
     * Get a room by ID.
     */
    public ChatRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    /**
     * List all rooms as a formatted string.
     */
    public String listRooms() {
        if (rooms.isEmpty()) {
            return "No rooms available. Create one!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Available Rooms:\n");
        for (ChatRoom room : rooms.values()) {
            sb.append("  - ").append(room.getRoomId())
              .append(" (Creator: ").append(room.getCreator())
              .append(", Members: ").append(room.getObserverCount())
              .append(")\n");
        }
        return sb.toString().trim();
    }

    /**
     * Remove a client from ALL rooms. Used on disconnect.
     */
    public void removeClientFromAllRooms(ChatRoomObserver observer) {
        for (ChatRoom room : rooms.values()) {
            if (room.hasObserver(observer)) {
                room.removeObserver(observer);

                // Notify remaining members
                common.Message notice = new common.Message(
                    common.MessageType.SYSTEM_MESSAGE, "SERVER",
                    observer.getObserverName() + " has disconnected.");
                notice.setRoomId(room.getRoomId());
                notice.stampTimestamp();
                room.notifyObservers(notice);
            }
        }
    }
}