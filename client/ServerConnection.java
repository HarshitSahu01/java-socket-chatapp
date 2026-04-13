package client;

import common.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerConnection {

    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private final AtomicBoolean connected;

    public ServerConnection() {
        this.connected = new AtomicBoolean(false);
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.flush();
        inputStream = new ObjectInputStream(socket.getInputStream());
        connected.set(true);
        System.out.println("[Connection] Connected to " + host + ":" + port);
    }

    public synchronized void send(Message msg) throws IOException {
        if (!connected.get()) throw new IOException("Not connected to server.");
        outputStream.writeObject(msg);
        outputStream.flush();
        outputStream.reset();
    }

    public Message receive() throws IOException, ClassNotFoundException {
        if (!connected.get()) throw new IOException("Not connected to server.");
        return (Message) inputStream.readObject();
    }

    public void disconnect() {
        connected.set(false);
        try { if (inputStream != null) inputStream.close(); } catch (IOException e) {}
        try { if (outputStream != null) outputStream.close(); } catch (IOException e) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) {}
        System.out.println("[Connection] Disconnected.");
    }

    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }
}