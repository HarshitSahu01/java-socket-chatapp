package server;

import common.Config;
import common.ProtocolConstants;

import java.io.File;

/**
 * Entry point for the server application.
 * Loads config, creates server, registers shutdown hook.
 */
public class ServerDriver {

    public static void main(String[] args) {
        System.out.println("=== Chat Server Starting ===");

        // 1. Check if config file exists
        String configPath = "resources/server.env";
        File configFile = new File(configPath);

        if (!configFile.exists()) {
            System.err.println("ERROR: Configuration file not found: "
                               + configPath);
            System.err.println("Please create resources/server.env with:");
            System.err.println("  PORT=9000");
            System.err.println("  SECRET=your_secret_key");
            System.exit(1);
        }

        // 2. Load config (Singleton)
        Config config = Config.getInstance(configPath);

        // 3. Validate required config
        if (!config.has("SECRET") || config.get("SECRET").isEmpty()) {
            System.err.println("ERROR: SECRET not defined in server.env");
            System.exit(1);
        }

        int port = config.getInt("PORT", ProtocolConstants.DEFAULT_PORT);
        String secret = config.get("SECRET");

        // 4. Create server with proxy
        ChatServer chatServer = new ChatServer(port);
        AuthProxyServer authProxy = new AuthProxyServer(chatServer, secret);

        // 5. Register shutdown hook for Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            authProxy.shutdown();
        }));

        // 6. Start server (blocking call)
        authProxy.start();
    }
}