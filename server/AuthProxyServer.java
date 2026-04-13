package server;

import common.Config;
import common.Message;

/**
 * Proxy Pattern — Protection Proxy.
 *
 * Wraps the ChatServer and provides authentication.
 * All first-messages must contain the correct secret.
 */
public class AuthProxyServer {

    private final ChatServer realServer;
    private final String secret;

    public AuthProxyServer(ChatServer realServer, String secret) {
        this.realServer = realServer;
        this.secret = secret;
        // Give the real server a reference back to us
        this.realServer.setAuthProxy(this);
    }

    /**
     * Start the wrapped server.
     */
    public void start() {
        System.out.println("[AuthProxy] Authentication proxy active.");
        System.out.println("[AuthProxy] Secret loaded: "
                           + secret.substring(0, Math.min(3, secret.length()))
                           + "***");
        realServer.start();
    }

    /**
     * Shutdown the wrapped server.
     */
    public void shutdown() {
        realServer.shutdown();
    }

    /**
     * Authenticate a message by checking its secret field.
     *
     * @param msg The incoming message.
     * @return true if the secret matches, false otherwise.
     */
    public boolean authenticate(Message msg) {
        if (msg == null || msg.getSecret() == null) {
            return false;
        }
        return secret.equals(msg.getSecret());
    }
}