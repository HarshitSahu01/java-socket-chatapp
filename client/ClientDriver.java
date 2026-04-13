package client;

import common.Config;
import common.ProtocolConstants;

import java.io.File;
import java.util.Scanner;

public class ClientDriver {

    public static void main(String[] args) {
        String configPath = "resources/client.env";

        if (!new File(configPath).exists()) {
            System.err.println("ERROR: Configuration file not found: " + configPath);
            System.err.println("Please create resources/client.env with HOST, PORT, and SECRET.");
            System.exit(1);
        }

        Config config = Config.getInstance(configPath);

        if (!config.has("SECRET") || config.get("SECRET").isEmpty()) {
            System.err.println("ERROR: SECRET not defined in client.env");
            System.exit(1);
        }

        String host = config.get("HOST").isEmpty() ? ProtocolConstants.DEFAULT_HOST : config.get("HOST");
        int port = config.getInt("PORT", ProtocolConstants.DEFAULT_PORT);
        String secret = config.get("SECRET");

        Scanner scanner = new Scanner(System.in);
        String username = "";
        while (username.trim().isEmpty()) {
            System.out.print("Enter your username: ");
            username = scanner.nextLine();
            if (username.trim().isEmpty()) System.out.println("Username cannot be empty. Try again.");
        }

        System.out.println("Connecting as '" + username.trim() + "' to " + host + ":" + port + "...");
        new ChatClient(username.trim(), host, port, secret).start();
        System.out.println("[Client] Application terminated.");
    }
}