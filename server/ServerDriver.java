package server;

import common.Config;
import common.ProtocolConstants;

import java.io.File;

public class ServerDriver {

    public static void main(String[] args) {
        String configPath = "resources/server.env";

        if (!new File(configPath).exists()) {
            System.err.println("ERROR: Configuration file not found: " + configPath);
            System.err.println("Please create resources/server.env with PORT and SECRET.");
            System.exit(1);
        }

        Config config = Config.getInstance(configPath);

        if (!config.has("SECRET") || config.get("SECRET").isEmpty()) {
            System.err.println("ERROR: SECRET not defined in server.env");
            System.exit(1);
        }

        int port = config.getInt("PORT", ProtocolConstants.DEFAULT_PORT);
        String secret = config.get("SECRET");

        ChatServer chatServer = new ChatServer(port);
        AuthProxyServer authProxy = new AuthProxyServer(chatServer, secret);

        Runtime.getRuntime().addShutdownHook(new Thread(authProxy::shutdown));

        authProxy.start();
    }
}