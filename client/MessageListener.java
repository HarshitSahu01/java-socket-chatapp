package client;

import common.Message;

import java.io.IOException;

/**
 * Runs on a separate daemon thread.
 * Continuously reads incoming messages from the server
 * and passes them to the ChatClient for handling.
 */
public class MessageListener implements Runnable {

    private final ServerConnection connection;
    private final ChatClient chatClient;
    private volatile boolean running;

    public MessageListener(ServerConnection connection,
                           ChatClient chatClient) {
        this.connection = connection;
        this.chatClient = chatClient;
        this.running = true;
    }

    @Override
    public void run() {
        while (running && connection.isConnected()) {
            try {
                Message msg = connection.receive();
                if (msg != null) {
                    chatClient.handleIncoming(msg);
                }
            } catch (IOException e) {
                if (running) {
                    System.out.println(
                        "\n[Listener] Lost connection to server: "
                        + e.getMessage());
                    chatClient.onConnectionLost();
                }
                break;
            } catch (ClassNotFoundException e) {
                System.err.println("[Listener] Received unknown object type.");
            }
        }
    }

    /**
     * Stop the listener.
     */
    public void stop() {
        running = false;
    }
}