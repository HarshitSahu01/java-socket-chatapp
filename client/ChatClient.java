package client;

import common.Message;
import common.MessageType;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatClient {

    private final String username;
    private final String host;
    private final int port;
    private final String secret;
    private final ServerConnection connection;
    private MessageListener listener;
    private MenuHandler menuHandler;
    private final AtomicBoolean running;
    private final AtomicBoolean inChatMode;
    private String currentRoom;

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public ChatClient(String username, String host, int port, String secret) {
        this.username = username;
        this.host = host;
        this.port = port;
        this.secret = secret;
        this.connection = new ServerConnection();
        this.running = new AtomicBoolean(false);
        this.inChatMode = new AtomicBoolean(false);
    }

    public void start() {
        try {
            connection.connect(host, port);

            connection.send(new Message(MessageType.CONNECT, secret, username, null, null));

            Message response = connection.receive();
            if (response.getType() == MessageType.ERROR) {
                System.out.println("[Client] Connection rejected: " + response.getContent());
                connection.disconnect();
                return;
            }

            System.out.println("[Server] " + response.getContent());
            running.set(true);

            listener = new MessageListener(connection, this);
            Thread listenerThread = new Thread(listener, "MessageListener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            menuHandler = new MenuHandler(this);
            menuHandler.run();

        } catch (IOException e) {
            System.err.println("[Client] Connection failed: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("[Client] Protocol error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    public void handleIncoming(Message msg) {
        if (msg == null) return;
        String timeStr = TIME_FORMAT.format(new Date(msg.getTimestamp()));

        switch (msg.getType()) {
            case CHAT_MESSAGE:
                menuHandler.displayMessage("[" + timeStr + "] " + msg.getSender() + ": " + msg.getContent());
                break;

            case SYSTEM_MESSAGE:
                menuHandler.displayMessage("[" + timeStr + "] *** " + msg.getContent() + " ***");
                if (msg.getContent() != null && msg.getContent().contains("has been deleted")) {
                    inChatMode.set(false);
                    currentRoom = null;
                    System.out.println("\nReturning to main menu...");
                }
                break;

            case ROOM_LIST:
                menuHandler.displayMessage("\n" + msg.getContent());
                break;

            case ACK:
                menuHandler.displayMessage("[Server] " + msg.getContent());
                if (msg.getRoomId() != null && msg.getContent() != null && msg.getContent().startsWith("You joined")) {
                    currentRoom = msg.getRoomId();
                    inChatMode.set(true);
                    menuHandler.showChatModeHeader(currentRoom);
                }
                if (msg.getContent() != null && msg.getContent().startsWith("You left")) {
                    currentRoom = null;
                    inChatMode.set(false);
                }
                break;

            case ERROR:
                menuHandler.displayMessage("[ERROR] " + msg.getContent());
                break;

            default:
                menuHandler.displayMessage("[Unknown] " + msg.toString());
        }
    }

    public void onConnectionLost() {
        running.set(false);
        inChatMode.set(false);
    }

    public void listRooms() {
        send(new Message(MessageType.LIST_ROOMS, secret, username, null, null));
    }

    public void createRoom(String roomId) {
        send(new Message(MessageType.CREATE_ROOM, secret, username, roomId, null));
    }

    public void joinRoom(String roomId) {
        send(new Message(MessageType.JOIN_ROOM, secret, username, roomId, null));
    }

    public void leaveRoom() {
        if (currentRoom == null) { System.out.println("[Client] You are not in any room."); return; }
        send(new Message(MessageType.LEAVE_ROOM, secret, username, currentRoom, null));
    }

    public void deleteRoom(String roomId) {
        send(new Message(MessageType.DELETE_ROOM, secret, username, roomId, null));
    }

    public void sendChat(String text) {
        if (currentRoom == null) { System.out.println("[Client] You are not in any room."); return; }
        send(new Message(MessageType.CHAT_MESSAGE, secret, username, currentRoom, text));
    }

    public void quit() {
        send(new Message(MessageType.DISCONNECT, secret, username, null, null));
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        running.set(false);
    }

    private void send(Message msg) {
        try {
            connection.send(msg);
        } catch (IOException e) {
            System.err.println("[Client] Failed to send message: " + e.getMessage());
            onConnectionLost();
        }
    }

    private void cleanup() {
        running.set(false);
        if (listener != null) listener.stop();
        connection.disconnect();
        System.out.println("[Client] Goodbye!");
    }

    public boolean isRunning()    { return running.get(); }
    public boolean isInChatMode() { return inChatMode.get(); }
    public String getUsername()   { return username; }
}