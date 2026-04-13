package client;

import java.util.Scanner;

public class MenuHandler {
    private final Scanner scanner;
    private final ChatClient chatClient;

    public MenuHandler(ChatClient chatClient) {
        this.scanner = new Scanner(System.in);
        this.chatClient = chatClient;
    }

    public void run() {
        while (chatClient.isRunning()) {
            if (chatClient.isInChatMode()) {
                handleChatMode();
            } else {
                showMainMenu();
                handleMainMenuInput();
            }
        }
    }

    private void showMainMenu() {
        System.out.println();
        System.out.println("╔══════════════════════════════╗");
        System.out.println("║        MAIN MENU             ║");
        System.out.println("╠══════════════════════════════╣");
        System.out.println("║  1. List Chat Rooms          ║");
        System.out.println("║  2. Create Chat Room         ║");
        System.out.println("║  3. Join Chat Room           ║");
        System.out.println("║  4. Delete Chat Room         ║");
        System.out.println("║  5. Quit                     ║");
        System.out.println("╚══════════════════════════════╝");
        System.out.print("Choose an option: ");
    }

    private void handleMainMenuInput() {
        String input = readLine();
        if (input == null) return;

        switch (input.trim()) {
            case "1":
                chatClient.listRooms();
                pause();
                break;
            case "2":
                System.out.print("Enter room name: ");
                String roomName = readLine();
                if (roomName != null && !roomName.trim().isEmpty()) {
                    chatClient.createRoom(roomName.trim());
                    pause();
                }
                break;
            case "3":
                System.out.print("Enter room name to join: ");
                String joinRoom = readLine();
                if (joinRoom != null && !joinRoom.trim().isEmpty()) {
                    chatClient.joinRoom(joinRoom.trim());
                }
                break;
            case "4":
                System.out.print("Enter room name to delete: ");
                String deleteRoom = readLine();
                if (deleteRoom != null && !deleteRoom.trim().isEmpty()) {
                    chatClient.deleteRoom(deleteRoom.trim());
                    pause();
                }
                break;
            case "5":
                chatClient.quit();
                break;
            default:
                System.out.println("Invalid option. Try again.");
        }
    }

    private void handleChatMode() {
        String input = readLine();
        if (input == null) return;

        if (input.trim().equalsIgnoreCase("/leave")) {
            chatClient.leaveRoom();
        } else if (input.trim().equalsIgnoreCase("/help")) {
            System.out.println("Commands: /leave - Leave room | /help - Show this");
        } else if (!input.trim().isEmpty()) {
            chatClient.sendChat(input);
        }
    }

    public synchronized void displayMessage(String text) {
        System.out.println(text);
    }

    public void showChatModeHeader(String roomId) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║  Joined Room: " + padRight(roomId, 23) + "║");
        System.out.println("║  Type /leave to exit, /help for help ║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    private String readLine() {
        try {
            if (scanner.hasNextLine()) return scanner.nextLine();
        } catch (Exception e) {}
        return null;
    }

    private void pause() {
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private String padRight(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        return String.format("%-" + n + "s", s);
    }
}