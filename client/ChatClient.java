package client;

import common.Config;
import common.Message;
import common.MessageType;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main client controller.
 * Coordinates between ServerConnection, MessageListener,
 * and MenuHandler.
 */
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

    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm:ss");

    public ChatClient(String username, String host, int port, String secret) {
        this.username = username;
        this.host = host;
        this.port = port;
        this.secret = secret;
        this.connection = new ServerConnection();
        this.running = new AtomicBoolean(false);
        this.inChatMode = new AtomicBoolean(false);
        this.currentRoom = null;
    }

    /**
     * Start the client — connect, authenticate, and launch menu.
     */
    public void start() {
        try {
            // 1. Connect to server
            connection.connect(host, port);

            // 2. Send CONNECT message with secret
            Message connectMsg = new Message(MessageType.CONNECT,
                    secret, username, null, null);
            connection.send(connectMsg);

            // 3. Wait for response
            Message response = connection.receive();
            if (response.getType() == MessageType.ERROR) {
                System.out.println("[Client] Connection rejected: "
                                   + response.getContent());
                connection.disconnect();
                return;
            }

            System.out.println("[Server] " + response.getContent());
            running.set(true);

            // 4. Start message listener thread (daemon)
            listener = new MessageListener(connection, this);
            Thread listenerThread = new Thread(listener, "MessageListener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            // 5. Launch menu (runs on main thread)
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

    /**
     * Handle incoming message from the listener thread.
     */
    public void handleIncoming(Message msg) {
        if (msg == null) return;

        String timeStr = TIME_FORMAT.format(new Date(msg.getTimestamp()));

        switch (msg.getType()) {
            case CHAT_MESSAGE:
                if (menuHandler != null) {
                    menuHandler.displayMessage(
                        "[" + timeStr + "] " + msg.getSender()
                        + ": " + msg.getContent());
                }
                break;

            case SYSTEM_MESSAGE:
                if (menuHandler != null) {
                    menuHandler.displayMessage(
                        "[" + timeStr + "] *** " + msg.getContent() + " ***");
                }
                // If room was deleted while we're in it
                if (msg.getContent() != null
                        && msg.getContent().contains("has been deleted")) {
                    inChatMode.set(false);
                    currentRoom = null;
                    System.out.println(
                        "\nReturning to main menu...");
                }
                break;

            case ROOM_LIST:
                if (menuHandler != null) {
                    menuHandler.displayMessage("\n" + msg.getContent());
                }
                break;

            case ACK:
                if (menuHandler != null) {
                    menuHandler.displayMessage(
                        "[Server] " + msg.getContent());
                }
                // If ACK is for joining a room, enter chat mode
                if (msg.getRoomId() != null
                        && msg.getContent() != null
                        && msg.getContent().startsWith("You joined")) {
                    currentRoom = msg.getRoomId();
                    inChatMode.set(true);
                    menuHandler.showChatModeHeader(currentRoom);
                }
                // If ACK is for leaving a room
                if (msg.getContent() != null
                        && msg.getContent().startsWith("You left")) {
                    currentRoom = null;
                    inChatMode.set(false);
                }
                break;

            case ERROR:
                if (menuHandler != null) {
                    menuHandler.displayMessage(
                        "[ERROR] " + msg.getContent());
                }
                break;

            default:
                if (menuHandler != null) {
                    menuHandler.displayMessage(
                        "[Unknown] " + msg.toString());
                }
                break;
        }
    }

    /**
     * Called by listener when connection is lost.
     */
    public void onConnectionLost() {
        running.set(false);
        inChatMode.set(false);
    }

    // --- Actions (called from MenuHandler) ---

    public void listRooms() {
        sendToServer(new Message(MessageType.LIST_ROOMS, secret,
                                 username, null, null));
    }

    public void createRoom(String roomId) {
        Message msg = new Message(MessageType.CREATE_ROOM, secret,
                                  username, roomId, null);
        sendToServer(msg);
    }

    public void joinRoom(String roomId) {
        Message msg = new Message(MessageType.JOIN_ROOM, secret,
                                  username, roomId, null);
        sendToServer(msg);
    }

    public void leaveRoom() {
        if (currentRoom == null) {
            System.out.println("[Client] You are not in any room.");
            return;
        }
        Message msg = new Message(MessageType.LEAVE_ROOM, secret,
                                  username, currentRoom, null);
        sendToServer(msg);
    }

    public void deleteRoom(String roomId) {
        Message msg = new Message(MessageType.DELETE_ROOM, secret,
                                  username, roomId, null);
        sendToServer(msg);
    }

    public void sendChat(String text) {
        if (currentRoom == null) {
            System.out.println("[Client] You are not in any room.");
            return;
        }
        Message msg = new Message(MessageType.CHAT_MESSAGE, secret,
                                  username, currentRoom, text);
        sendToServer(msg);
    }

    public void quit() {
        Message msg = new Message(MessageType.DISCONNECT, secret,
                                  username, null, null);
        sendToServer(msg);

        // Small delay to let ACK arrive
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        running.set(false);
    }

    // --- Helpers ---

    private void sendToServer(Message msg) {
        try {
            connection.send(msg);
        } catch (IOException e) {
            System.err.println("[Client] Failed to send message: "
                               + e.getMessage());
            onConnectionLost();
        }
    }

    private void cleanup() {
        running.set(false);
        if (listener != null) {
            listener.stop();
        }
        connection.disconnect();
        System.out.println("[Client] Goodbye!");
    }

    // --- State queries (used by MenuHandler) ---

    public boolean isRunning() {
        return running.get();
    }

    public boolean isInChatMode() {
        return inChatMode.get();
    }

    public String getUsername() {
        return username;
    }
}