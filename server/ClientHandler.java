package server;

import common.Message;
import common.MessageType;
import server.command.Command;
import server.command.CommandFactory;
import server.observer.ChatRoomObserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Handles a single client connection.
 * Implements ChatRoomObserver so it can receive room messages.
 * Runs in its own thread (submitted to thread pool).
 */
public class ClientHandler implements Runnable, ChatRoomObserver {

    private final Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String username;
    private boolean authenticated;
    private volatile boolean running;
    private final ChatServer server;
    private final AuthProxyServer authProxy;
    private String currentRoom;

    public ClientHandler(Socket socket, ChatServer server,
                         AuthProxyServer authProxy) {
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
            // IMPORTANT: Output stream must be created BEFORE input stream
            // to avoid deadlock (both sides waiting to read headers)
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());

            System.out.println("[ClientHandler] New connection from: "
                               + socket.getRemoteSocketAddress());

            while (running) {
                // Read message from client
                Message msg;
                try {
                    msg = (Message) inputStream.readObject();
                } catch (ClassNotFoundException e) {
                    System.err.println("[ClientHandler] Invalid object received.");
                    continue;
                }

                if (msg == null) {
                    continue;
                }

                // If not authenticated yet, first message must be CONNECT
                if (!authenticated) {
                    if (!authProxy.authenticate(msg)) {
                        Message error = new Message(MessageType.ERROR,
                                "SERVER", "Authentication failed. Invalid secret.");
                        error.stampTimestamp();
                        sendMessage(error);
                        disconnect();
                        return;
                    }

                    if (msg.getType() != MessageType.CONNECT) {
                        Message error = new Message(MessageType.ERROR,
                                "SERVER",
                                "First message must be CONNECT.");
                        error.stampTimestamp();
                        sendMessage(error);
                        disconnect();
                        return;
                    }

                    authenticated = true;
                    // Fall through to command execution for CONNECT
                }

                // Create and execute the command
                Command command = CommandFactory.create(msg, this, server);
                command.execute();

                // If it was a disconnect command, stop the loop
                if (msg.getType() == MessageType.DISCONNECT) {
                    return;
                }
            }

        } catch (IOException e) {
            System.out.println("[ClientHandler] Connection lost: "
                               + (username != null ? username : "unknown")
                               + " - " + e.getMessage());
        } finally {
            // Cleanup on any exit
            cleanup();
        }
    }

    /**
     * Send a message to this client. Synchronized to prevent
     * concurrent writes from multiple threads (e.g., room broadcast
     * + direct response).
     */
    public synchronized void sendMessage(Message msg) {
        try {
            if (outputStream != null && !socket.isClosed()) {
                outputStream.writeObject(msg);
                outputStream.flush();
                outputStream.reset(); // Prevent caching of objects
            }
        } catch (IOException e) {
            System.err.println("[ClientHandler] Failed to send message to "
                               + username + ": " + e.getMessage());
            running = false;
        }
    }

    /**
     * Disconnect this client (close socket and streams).
     */
    public void disconnect() {
        running = false;
    }

    /**
     * Cleanup resources.
     */
    private void cleanup() {
        running = false;

        // Remove from all rooms
        if (username != null) {
            server.getRoomManager().removeClientFromAllRooms(this);
            server.removeClient(username);
        }

        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException e) { /* ignore */ }

        try {
            if (outputStream != null) outputStream.close();
        } catch (IOException e) { /* ignore */ }

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) { /* ignore */ }

        System.out.println("[ClientHandler] Cleaned up: "
                           + (username != null ? username : "unknown"));
    }

    // --- Observer Pattern: onMessage ---

    @Override
    public void onMessage(Message msg) throws IOException {
        if (outputStream == null || socket.isClosed()) {
            throw new IOException("Client connection is closed.");
        }
        sendMessage(msg);
    }

    @Override
    public String getObserverName() {
        return username != null ? username : "unknown";
    }

    // --- Getters and Setters ---

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }
}