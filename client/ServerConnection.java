package client;

import common.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the raw socket connection to the server.
 * Handles connect, send, receive, and disconnect operations.
 */
public class ServerConnection {

    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private final AtomicBoolean connected;

    public ServerConnection() {
        this.connected = new AtomicBoolean(false);
    }

    /**
     * Connect to the server.
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);

        // Output MUST be created before Input to avoid deadlock
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.flush();
        inputStream = new ObjectInputStream(socket.getInputStream());

        connected.set(true);
        System.out.println("[Connection] Connected to " + host + ":" + port);
    }

    /**
     * Send a message to the server. Synchronized for thread safety.
     */
    public synchronized void send(Message msg) throws IOException {
        if (!connected.get()) {
            throw new IOException("Not connected to server.");
        }
        outputStream.writeObject(msg);
        outputStream.flush();
        outputStream.reset(); // Prevent object caching
    }

    /**
     * Receive a message from the server. Blocks until available.
     */
    public Message receive() throws IOException, ClassNotFoundException {
        if (!connected.get()) {
            throw new IOException("Not connected to server.");
        }
        return (Message) inputStream.readObject();
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        connected.set(false);

        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException e) { /* ignore */ }

        try {
            if (outputStream != null) outputStream.close();
        } catch (IOException e) { /* ignore */ }

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) { /* ignore */ }

        System.out.println("[Connection] Disconnected.");
    }

    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }
}