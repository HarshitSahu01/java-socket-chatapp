package client;

import common.Config;
import common.ProtocolConstants;

import java.io.File;
import java.util.Scanner;

/**
 * Entry point for the client application.
 * Loads config, prompts for username, and starts the ChatClient.
 */
public class ClientDriver {

    public static void main(String[] args) {
        System.out.println("=== Chat Client Starting ===");

        // 1. Check if config file exists
        String configPath = "resources/client.env";
        File configFile = new File(configPath);

        if (!configFile.exists()) {
            System.err.println("ERROR: Configuration file not found: "
                               + configPath);
            System.err.println("Please create resources/client.env with:");
            System.err.println("  HOST=localhost");
            System.err.println("  PORT=9000");
            System.err.println("  SECRET=your_secret_key");
            System.exit(1);
        }

        // 2. Load config (Singleton)
        Config config = Config.getInstance(configPath);

        // 3. Validate required config
        if (!config.has("SECRET") || config.get("SECRET").isEmpty()) {
            System.err.println("ERROR: SECRET not defined in client.env");
            System.exit(1);
        }

        String host = config.get("HOST");
        if (host.isEmpty()) {
            host = ProtocolConstants.DEFAULT_HOST;
        }

        int port = config.getInt("PORT", ProtocolConstants.DEFAULT_PORT);
        String secret = config.get("SECRET");

        // 4. Prompt for username
        Scanner scanner = new Scanner(System.in);
        String username = "";
        while (username.trim().isEmpty()) {
            System.out.print("Enter your username: ");
            username = scanner.nextLine();
            if (username.trim().isEmpty()) {
                System.out.println("Username cannot be empty. Try again.");
            }
        }
        username = username.trim();

        System.out.println("Connecting as '" + username + "' to "
                           + host + ":" + port + "...");

        // 5. Create and start the ChatClient
        ChatClient chatClient = new ChatClient(username, host, port, secret);

        // 6. Register shutdown hook for Ctrl+C on client side
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Client] Shutdown hook triggered.");
        }));

        // 7. Start the client (blocking call — runs until quit)
        chatClient.start();

        System.out.println("[Client] Application terminated.");
    }
}