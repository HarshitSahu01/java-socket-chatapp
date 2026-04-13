package server;

import common.Message;
import common.MessageType;
import server.observer.ChatRoomObserver;
import server.observer.ChatRoomSubject;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer Pattern — Concrete Subject.
 *
 * Represents a chat room. Maintains a list of observers (clients).
 * When a message arrives, it notifies all observers.
 * If notification fails (IOException), the observer is removed.
 */
public class ChatRoom implements ChatRoomSubject {

    private final String roomId;
    private final String creator;
    private final List<ChatRoomObserver> observers;

    public ChatRoom(String roomId, String creator) {
        this.roomId = roomId;
        this.creator = creator;
        this.observers = new CopyOnWriteArrayList<>();
    }

    @Override
    public void addObserver(ChatRoomObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(ChatRoomObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Message msg) {
        for (ChatRoomObserver observer : observers) {
            try {
                observer.onMessage(msg);
            } catch (Exception e) {
                // Observer is dead — remove it
                System.out.println("[ChatRoom " + roomId
                        + "] Removing dead observer: "
                        + observer.getObserverName());
                observers.remove(observer);
            }
        }
    }

    /**
     * Notify all observers that the room is being deleted,
     * then clear the observer list.
     */
    public void notifyAndClear(String reason) {
        Message notice = new Message(MessageType.SYSTEM_MESSAGE,
                "SERVER", reason);
        notice.setRoomId(roomId);
        notice.stampTimestamp();

        for (ChatRoomObserver observer : observers) {
            try {
                observer.onMessage(notice);
            } catch (Exception e) {
                // Ignore — we are clearing anyway
            }
        }
        observers.clear();
    }

    public boolean hasObserver(ChatRoomObserver observer) {
        return observers.contains(observer);
    }

    public int getObserverCount() {
        return observers.size();
    }

    public String getRoomId() {
        return roomId;
    }

    public String getCreator() {
        return creator;
    }

    public List<ChatRoomObserver> getObservers() {
        return observers;
    }
}