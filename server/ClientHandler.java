package server;

import common.Message;
import common.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String username;
    private boolean authenticated;
    private volatile boolean running;
    private final ChatServer server;
    private final AuthProxyServer authProxy;
    private String currentRoom;

    public ClientHandler(Socket socket, ChatServer server, AuthProxyServer authProxy) {
        this.socket = socket;
        this.server = server;
        this.authProxy = authProxy;
        this.authenticated = false;
        this.running = true;
        this.currentRoom = null;
    }

    @Override
    public void run() {
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());

            System.out.println("[Server] New connection from: " + socket.getRemoteSocketAddress());

            while (running) {
                Message msg;
                try {
                    msg = (Message) inputStream.readObject();
                } catch (ClassNotFoundException e) {
                    System.err.println("[Server] Invalid object received.");
                    continue;
                }

                if (msg == null) continue;

                if (!authenticated) {
                    if (!authProxy.authenticate(msg)) {
                        sendError("Authentication failed. Invalid secret.");
                        disconnect();
                        return;
                    }
                    if (msg.getType() != MessageType.CONNECT) {
                        sendError("First message must be CONNECT.");
                        disconnect();
                        return;
                    }
                    authenticated = true;
                }

                handleMessage(msg);

                if (msg.getType() == MessageType.DISCONNECT) return;
            }

        } catch (IOException e) {
            System.out.println("[Server] Connection lost: " + (username != null ? username : "unknown"));
        } finally {
            cleanup();
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {

            case CONNECT: {
                String name = msg.getSender();
                if (name == null || name.trim().isEmpty()) {
                    sendError("Username cannot be empty.");
                    return;
                }
                name = name.trim();
                if (server.isUsernameTaken(name)) {
                    sendError("Username '" + name + "' is already taken.");
                    return;
                }
                username = name;
                server.registerClient(username, this);
                System.out.println("[Server] User connected: " + username);
                sendAck("Welcome, " + username + "! You are now connected.", null);
                break;
            }

            case LIST_ROOMS: {
                Message response = new Message(MessageType.ROOM_LIST, "SERVER", server.getRoomManager().listRooms());
                response.stampTimestamp();
                sendMessage(response);
                break;
            }

            case CREATE_ROOM: {
                String roomId = msg.getRoomId();
                if (roomId == null || roomId.trim().isEmpty()) {
                    sendError("Room ID cannot be empty.");
                    return;
                }
                roomId = roomId.trim();
                ChatRoom room = server.getRoomManager().createRoom(roomId, username);
                if (room == null) {
                    sendError("Room '" + roomId + "' already exists.");
                    return;
                }
                System.out.println("[Server] Room created: " + roomId + " by " + username);
                sendAck("Room '" + roomId + "' created successfully.", null);
                break;
            }

            case JOIN_ROOM: {
                String roomId = msg.getRoomId();
                if (roomId == null || roomId.trim().isEmpty()) {
                    sendError("Room ID cannot be empty.");
                    return;
                }
                roomId = roomId.trim();

                if (currentRoom != null) {
                    ChatRoom oldRoom = server.getRoomManager().getRoom(currentRoom);
                    if (oldRoom != null) {
                        oldRoom.removeObserver(this);
                        broadcast(oldRoom, username + " has left the room.", currentRoom);
                    }
                    currentRoom = null;
                }

                ChatRoom room = server.getRoomManager().getRoom(roomId);
                if (room == null) {
                    sendError("Room '" + roomId + "' does not exist.");
                    return;
                }
                if (room.hasObserver(this)) {
                    sendError("You are already in room '" + roomId + "'.");
                    return;
                }

                room.addObserver(this);
                currentRoom = roomId;
                System.out.println("[Server] " + username + " joined room: " + roomId);
                sendAck("You joined room '" + roomId + "'.", roomId);
                broadcast(room, username + " has joined the room.", roomId);
                break;
            }

            case LEAVE_ROOM: {
                if (currentRoom == null) {
                    sendError("You are not in any room.");
                    return;
                }
                ChatRoom room = server.getRoomManager().getRoom(currentRoom);
                if (room != null) {
                    room.removeObserver(this);
                    broadcast(room, username + " has left the room.", currentRoom);
                }
                System.out.println("[Server] " + username + " left room: " + currentRoom);
                String leftRoom = currentRoom;
                currentRoom = null;
                sendAck("You left room '" + leftRoom + "'.", null);
                break;
            }

            case CHAT_MESSAGE: {
                if (currentRoom == null) {
                    sendError("You are not in any room. Join a room first.");
                    return;
                }
                ChatRoom room = server.getRoomManager().getRoom(currentRoom);
                if (room == null) {
                    sendError("Room no longer exists.");
                    currentRoom = null;
                    return;
                }
                Message broadcast = new Message(MessageType.CHAT_MESSAGE, username, msg.getContent());
                broadcast.setRoomId(currentRoom);
                broadcast.stampTimestamp();
                room.notifyObservers(broadcast);
                break;
            }

            case DELETE_ROOM: {
                String roomId = msg.getRoomId();
                if (roomId == null || roomId.trim().isEmpty()) {
                    sendError("Room ID cannot be empty.");
                    return;
                }
                roomId = roomId.trim();
                boolean deleted = server.getRoomManager().deleteRoom(roomId, username);
                if (!deleted) {
                    sendError("Cannot delete room '" + roomId + "'. It doesn't exist or you are not the creator.");
                    return;
                }
                System.out.println("[Server] Room deleted: " + roomId + " by " + username);
                sendAck("Room '" + roomId + "' deleted successfully.", null);
                break;
            }

            case DISCONNECT: {
                System.out.println("[Server] User disconnecting: " + username);
                server.getRoomManager().removeClientFromAllRooms(this);
                server.removeClient(username);
                sendAck("Goodbye, " + username + "!", null);
                disconnect();
                break;
            }

            default:
                sendError("Unknown command: " + msg.getType());
        }
    }

    private void sendAck(String content, String roomId) {
        Message ack = new Message(MessageType.ACK, "SERVER", content);
        ack.setRoomId(roomId);
        ack.stampTimestamp();
        sendMessage(ack);
    }

    private void sendError(String content) {
        Message err = new Message(MessageType.ERROR, "SERVER", content);
        err.stampTimestamp();
        sendMessage(err);
    }

    private void broadcast(ChatRoom room, String text, String roomId) {
        Message notice = new Message(MessageType.SYSTEM_MESSAGE, "SERVER", text);
        notice.setRoomId(roomId);
        notice.stampTimestamp();
        room.notifyObservers(notice);
    }

    public synchronized void sendMessage(Message msg) {
        try {
            if (outputStream != null && !socket.isClosed()) {
                outputStream.writeObject(msg);
                outputStream.flush();
                outputStream.reset();
            }
        } catch (IOException e) {
            System.err.println("[Server] Failed to send to " + username + ": " + e.getMessage());
            running = false;
        }
    }

    public void disconnect() {
        running = false;
    }

    private void cleanup() {
        running = false;
        if (username != null) {
            server.getRoomManager().removeClientFromAllRooms(this);
            server.removeClient(username);
        }
        try { if (inputStream != null) inputStream.close(); } catch (IOException e) {}
        try { if (outputStream != null) outputStream.close(); } catch (IOException e) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) {}
        System.out.println("[Server] Cleaned up: " + (username != null ? username : "unknown"));
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(String currentRoom) { this.currentRoom = currentRoom; }
}