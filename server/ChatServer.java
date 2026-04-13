package server;

import common.Config;
import common.ProtocolConstants;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The core chat server.
 * Accepts connections and assigns each to a ClientHandler thread.
 */
public class ChatServer {

    private final int port;
    private ServerSocket serverSocket;
    private final ConcurrentHashMap<String, ClientHandler> clients;
    private final ChatRoomManager roomManager;
    private final ExecutorService threadPool;
    private final AtomicBoolean running;
    private AuthProxyServer authProxy; // Set after construction

    public ChatServer(int port) {
        this.port = port;
        this.clients = new ConcurrentHashMap<>();
        this.roomManager = new ChatRoomManager();
        this.threadPool = Executors.newFixedThreadPool(50);
        this.running = new AtomicBoolean(false);
    }

    /**
     * Set the auth proxy reference (needed by ClientHandler).
     */
    public void setAuthProxy(AuthProxyServer authProxy) {
        this.authProxy = authProxy;
    }

    /**
     * Start the server — begin accepting connections.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running.set(true);
            System.out.println("========================================");
            System.out.println("  Chat Server started on port " + port);
            System.out.println("  Press Ctrl+C to shutdown.");
            System.out.println("========================================");

            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(
                            clientSocket, this, authProxy);
                    threadPool.submit(handler);
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("[Server] Error accepting connection: "
                                           + e.getMessage());
                    }
                    // If not running, the socket was closed for shutdown
                }
            }

        } catch (IOException e) {
            System.err.println("[Server] Could not start on port "
                               + port + ": " + e.getMessage());
        }
    }

    /**
     * Gracefully shutdown the server.
     */
    public void shutdown() {
        System.out.println("\n[Server] Shutting down...");
        running.set(false);

        // Close server socket to unblock accept()
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[Server] Error closing server socket: "
                               + e.getMessage());
        }

        // Disconnect all clients
        for (ClientHandler handler : clients.values()) {
            handler.disconnect();
        }
        clients.clear();

        // Shutdown thread pool
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        System.out.println("[Server] Shutdown complete.");
    }

    /**
     * Register a client by username.
     */
    public void registerClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        System.out.println("[Server] Registered: " + username
                           + " | Total clients: " + clients.size());
    }

    /**
     * Remove a client by username.
     */
    public void removeClient(String username) {
        if (username != null) {
            clients.remove(username);
            System.out.println("[Server] Removed: " + username
                               + " | Total clients: " + clients.size());
        }
    }

    /**
     * Check if a username is already in use.
     */
    public boolean isUsernameTaken(String username) {
        return clients.containsKey(username);
    }

    /**
     * Get the room manager.
     */
    public ChatRoomManager getRoomManager() {
        return roomManager;
    }
}