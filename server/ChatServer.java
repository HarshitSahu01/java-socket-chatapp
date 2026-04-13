package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatServer {

    private final int port;
    private ServerSocket serverSocket;
    private final ConcurrentHashMap<String, ClientHandler> clients;
    private final ChatRoomManager roomManager;
    private final ExecutorService threadPool;
    private final AtomicBoolean running;
    private AuthProxyServer authProxy;

    public ChatServer(int port) {
        this.port = port;
        this.clients = new ConcurrentHashMap<>();
        this.roomManager = new ChatRoomManager();
        this.threadPool = Executors.newFixedThreadPool(50);
        this.running = new AtomicBoolean(false);
    }

    public void setAuthProxy(AuthProxyServer authProxy) {
        this.authProxy = authProxy;
    }

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
                    threadPool.submit(new ClientHandler(clientSocket, this, authProxy));
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("[Server] Error accepting connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Server] Could not start on port " + port + ": " + e.getMessage());
        }
    }

    public void shutdown() {
        System.out.println("\n[Server] Shutting down...");
        running.set(false);

        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            System.err.println("[Server] Error closing socket: " + e.getMessage());
        }

        for (ClientHandler handler : clients.values()) handler.disconnect();
        clients.clear();

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) threadPool.shutdownNow();
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        System.out.println("[Server] Shutdown complete.");
    }

    public void registerClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        System.out.println("[Server] Registered: " + username + " | Total clients: " + clients.size());
    }

    public void removeClient(String username) {
        if (username != null) {
            clients.remove(username);
            System.out.println("[Server] Removed: " + username + " | Total clients: " + clients.size());
        }
    }

    public boolean isUsernameTaken(String username) {
        return clients.containsKey(username);
    }

    public ChatRoomManager getRoomManager() {
        return roomManager;
    }
}