package server;

import common.Message;

public class AuthProxyServer {

    private final ChatServer realServer;
    private final String secret;

    public AuthProxyServer(ChatServer realServer, String secret) {
        this.realServer = realServer;
        this.secret = secret;
        this.realServer.setAuthProxy(this);
    }

    public void start() {
        System.out.println("[AuthProxy] Authentication proxy active.");
        realServer.start();
    }

    public void shutdown() {
        realServer.shutdown();
    }

    public boolean authenticate(Message msg) {
        return msg != null && msg.getSecret() != null && secret.equals(msg.getSecret());
    }
}