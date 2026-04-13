package server;

import common.Message;
import common.MessageType;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatRoom {

    private final String roomId;
    private final String creator;
    private final List<ClientHandler> observers;

    public ChatRoom(String roomId, String creator) {
        this.roomId = roomId;
        this.creator = creator;
        this.observers = new CopyOnWriteArrayList<>();
    }

    public void addObserver(ClientHandler client) {
        if (!observers.contains(client)) {
            observers.add(client);
        }
    }

    public void removeObserver(ClientHandler client) {
        observers.remove(client);
    }

    public void notifyObservers(Message msg) {
        for (ClientHandler client : observers) {
            client.sendMessage(msg);
        }
    }

    public void notifyAndClear(String reason) {
        Message notice = new Message(MessageType.SYSTEM_MESSAGE, "SERVER", reason);
        notice.setRoomId(roomId);
        notice.stampTimestamp();
        for (ClientHandler client : observers) {
            client.sendMessage(notice);
        }
        observers.clear();
    }

    public boolean hasObserver(ClientHandler client) { return observers.contains(client); }
    public int getObserverCount()                    { return observers.size(); }
    public String getRoomId()                        { return roomId; }
    public String getCreator()                       { return creator; }
    public List<ClientHandler> getObservers()        { return observers; }
}