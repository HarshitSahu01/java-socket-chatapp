

```markdown
# 💬 Java Socket-Based Chat Application

A real-time, multi-client chat application built with Java sockets, demonstrating **7 design patterns** in a clean, modular architecture. Clients connect to a centralized server, authenticate via a shared secret, create/join chat rooms, and exchange messages in real time.

---

## 📑 Table of Contents

- [Features](#-features)
- [Design Patterns Implemented](#-design-patterns-implemented)
- [Project Structure](#-project-structure)
- [Architecture](#-architecture)
  - [Architecture Diagram](#architecture-diagram)
  - [Architecture Description](#architecture-description)
  - [Communication Flow](#communication-flow)
  - [Threading Model](#threading-model)
- [Prerequisites](#-prerequisites)
- [Setup & Installation](#-setup--installation)
- [Configuration](#-configuration)
- [Compilation](#-compilation)
- [Execution](#-execution)
- [Usage Guide](#-usage-guide)
- [Design Patterns — Detailed Explanation](#-design-patterns--detailed-explanation)
  - [1. Singleton Pattern](#1-singleton-pattern)
  - [2. Proxy Pattern](#2-proxy-pattern)
  - [3. Observer Pattern](#3-observer-pattern)
  - [4. Command Pattern](#4-command-pattern)
  - [5. Factory Method Pattern](#5-factory-method-pattern)
  - [6. Template Method Pattern](#6-template-method-pattern)
  - [7. Composition Over Inheritance](#7-composition-over-inheritance)
- [Protocol Specification](#-protocol-specification)
- [Thread Safety Measures](#-thread-safety-measures)
- [Error Handling](#-error-handling)
- [Limitations & Future Improvements](#-limitations--future-improvements)
- [Contributors](#-contributors)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| **Real-time Messaging** | Instant message delivery across all room members via Observer pattern |
| **Multiple Chat Rooms** | Create, join, leave, and delete chat rooms dynamically |
| **Authentication** | Shared-secret based authentication via Proxy pattern |
| **Multi-client Support** | Thread pool handles up to 50 concurrent clients |
| **Graceful Shutdown** | Ctrl+C triggers clean disconnection of all clients and threads |
| **Dead Client Detection** | Automatic removal of disconnected clients from rooms |
| **Room Ownership** | Only the creator of a room can delete it |
| **Server-side Timestamps** | All messages are timestamped on the server for consistency |
| **Configurable** | All settings loaded from `.env` files via Singleton Config |

---

## 🎨 Design Patterns Implemented

| # | Pattern | Type | Where Implemented |
|---|---------|------|-------------------|
| 1 | **Singleton** | Creational | `Config.java` — single source of configuration |
| 2 | **Proxy** | Structural | `AuthProxyServer.java` — authentication gate |
| 3 | **Observer** | Behavioral | `ChatRoom.java` ↔ `ClientHandler.java` |
| 4 | **Command** | Behavioral | `Command.java` + all concrete commands |
| 5 | **Factory Method** | Creational | `CommandFactory.java` — creates commands by type |
| 6 | **Template Method** | Behavioral | `ClientHandler.run()` — standardized connection lifecycle |
| 7 | **Composition over Inheritance** | Structural Principle | `AuthProxyServer` wraps `ChatServer` |

---

## 📁 Project Structure

```
project/
│
├── 📂 common/                              # Shared between client & server
│   ├── Config.java                         # Singleton — loads .env configuration
│   ├── Message.java                        # Serializable message POJO
│   ├── MessageType.java                    # Enum — all protocol message types
│   └── ProtocolConstants.java              # Shared constants (port, timeouts)
│
├── 📂 server/                              # Server-side application
│   ├── ServerDriver.java                   # Entry point — boots server
│   ├── ChatServer.java                     # Core server — accepts connections
│   ├── AuthProxyServer.java                # Proxy — authenticates clients
│   ├── ClientHandler.java                  # Per-client thread — Observer
│   ├── ChatRoom.java                       # Chat room — Observable Subject
│   ├── ChatRoomManager.java                # Manages all chat rooms
│   │
│   ├── 📂 command/                         # Command Pattern classes
│   │   ├── Command.java                    # Command interface
│   │   ├── CommandFactory.java             # Factory — creates commands
│   │   ├── ConnectCommand.java             # Handles CONNECT
│   │   ├── DisconnectCommand.java          # Handles DISCONNECT
│   │   ├── JoinRoomCommand.java            # Handles JOIN_ROOM
│   │   ├── LeaveRoomCommand.java           # Handles LEAVE_ROOM
│   │   ├── CreateRoomCommand.java          # Handles CREATE_ROOM
│   │   ├── DeleteRoomCommand.java          # Handles DELETE_ROOM
│   │   ├── ListRoomsCommand.java           # Handles LIST_ROOMS
│   │   ├── SendMessageCommand.java         # Handles CHAT_MESSAGE
│   │   └── ErrorCommand.java              # Fallback for unknown types
│   │
│   └── 📂 observer/                        # Observer Pattern interfaces
│       ├── ChatRoomObserver.java            # Observer interface
│       └── ChatRoomSubject.java             # Subject interface
│
├── 📂 client/                              # Client-side application
│   ├── ClientDriver.java                   # Entry point — boots client
│   ├── ChatClient.java                     # Main client controller
│   ├── ServerConnection.java               # Socket connection manager
│   ├── MessageListener.java                # Daemon thread — receives messages
│   └── MenuHandler.java                    # Console UI handler
│
├── 📂 resources/                           # Configuration files
│   ├── server.env                          # Server configuration
│   └── client.env                          # Client configuration
│
├── compile.bat                             # Windows compilation script
├── compile.sh                              # Linux/Mac compilation script
└── README.md                               # This file
```

---

## 🏗️ Architecture

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT SIDE                              │
│                                                                 │
│  ┌──────────┐    ┌───────────┐    ┌────────────────────┐       │
│  │  Client   │───▶│  Chat     │───▶│  ServerConnection  │       │
│  │  Driver   │    │  Client   │    │  (Socket Manager)  │       │
│  └──────────┘    │           │    └────────┬───────────┘       │
│       │          │           │             │                    │
│       │          │           │◀────────────┤                    │
│       ▼          │           │    ┌────────┴───────────┐       │
│  ┌──────────┐    │           │    │  MessageListener   │       │
│  │  Config   │    │           │    │  (Daemon Thread)   │       │
│  │(Singleton)│    │           │    └────────────────────┘       │
│  └──────────┘    │           │                                  │
│                  │           │◀──── MenuHandler                 │
│                  └───────────┘      (Console UI)               │
│                                                                 │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                    TCP Socket
                  (ObjectStreams)
                          │
┌─────────────────────────┴───────────────────────────────────────┐
│                        SERVER SIDE                              │
│                                                                 │
│  ┌──────────┐    ┌────────────────┐    ┌──────────────┐        │
│  │  Server   │───▶│ AuthProxyServer│───▶│  ChatServer   │        │
│  │  Driver   │    │ (Proxy Pattern)│    │  (Real Server)│        │
│  └──────────┘    └────────────────┘    └──────┬───────┘        │
│       │                                       │                 │
│       ▼                                       ▼                 │
│  ┌──────────┐                        ┌────────────────┐         │
│  │  Config   │                        │  Thread Pool   │         │
│  │(Singleton)│                        │  (50 threads)  │         │
│  └──────────┘                        └───┬───┬───┬────┘         │
│                                          │   │   │              │
│                            ┌─────────────┘   │   └──────┐      │
│                            ▼                 ▼          ▼      │
│                    ┌──────────────┐  ┌──────────┐ ┌─────────┐ │
│                    │ClientHandler │  │ClientHdlr│ │ClientHdl│ │
│                    │   (Alice)    │  │  (Bob)   │ │(Charlie)│ │
│                    └──────┬───────┘  └─────┬────┘ └────┬────┘ │
│                           │                │           │       │
│                           ▼                ▼           ▼       │
│                    ┌──────────────┐  ┌───────────────────┐     │
│                    │CommandFactory│  │  ChatRoomManager   │     │
│                    └──────┬───────┘  └─────────┬─────────┘     │
│                           │                    │                │
│                    ┌──────┴───────┐    ┌───────┴────────┐      │
│                    │   Command    │    │    ChatRoom     │      │
│                    │  (execute)   │    │   (Observer     │      │
│                    └──────────────┘    │    Subject)     │      │
│                                       └────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
```

### Architecture Description

The application follows a **client-server architecture** with clear separation of concerns:

#### Server Side

1. **ServerDriver** — Entry point. Loads configuration, validates required settings, creates the server stack, registers a JVM shutdown hook for graceful termination on `Ctrl+C`, and starts the server.

2. **AuthProxyServer (Proxy Pattern)** — Acts as a protection proxy wrapping the real `ChatServer`. Every client's first message must contain the correct shared secret. If authentication fails, the connection is immediately rejected. This prevents unauthorized access without modifying the core server logic.

3. **ChatServer** — The core server engine. Opens a `ServerSocket` on the configured port and enters an accept loop. Each incoming client connection is wrapped in a `ClientHandler` and submitted to a fixed-size thread pool (50 threads). Maintains a concurrent map of all connected clients by username.

4. **ClientHandler** — One per connected client. Runs in its own thread, continuously reading messages from the client's socket. Each incoming message is passed to the `CommandFactory`, which creates the appropriate `Command` object. The command is then executed. Also implements `ChatRoomObserver` so it can receive broadcast messages from any room it has joined.

5. **CommandFactory (Factory Pattern)** — A static factory that maps `MessageType` to concrete `Command` objects. Adding a new feature requires only a new command class and one line in the factory — no modification to existing code (Open/Closed Principle).

6. **Command Classes (Command Pattern)** — Each user action (connect, join room, send message, etc.) is encapsulated in its own command class with a single `execute()` method. Commands interact with `ChatServer`, `ChatRoomManager`, and `ChatRoom` to fulfill requests.

7. **ChatRoomManager** — Centralized manager for all chat rooms. Handles creation, deletion, lookup, and cleanup. Thread-safe via `ConcurrentHashMap`.

8. **ChatRoom (Observer Pattern)** — The Subject in the Observer pattern. Maintains a list of `ChatRoomObserver` instances (which are `ClientHandler` objects). When a message is broadcast, `notifyObservers()` iterates over all registered observers and calls `onMessage()`. If delivery fails (client disconnected), the observer is automatically removed.

#### Client Side

1. **ClientDriver** — Entry point. Loads configuration, prompts for a username, and creates/starts the `ChatClient`.

2. **ChatClient** — The main controller. Coordinates between `ServerConnection` (transport), `MessageListener` (incoming messages), and `MenuHandler` (user interface). Handles all business logic for sending requests and processing responses.

3. **ServerConnection** — Manages the raw TCP socket connection. Provides thread-safe `send()`, blocking `receive()`, and clean `disconnect()` operations. Isolated from business logic for single responsibility.

4. **MessageListener** — A daemon thread that continuously reads incoming messages from the server and delegates them to `ChatClient.handleIncoming()`. Runs concurrently with the menu thread so the client can send and receive simultaneously.

5. **MenuHandler** — Handles all console I/O. Displays menus, reads user input, and delegates actions to `ChatClient`. Separated from client logic so the UI could be swapped (e.g., to a GUI) without touching business logic.

#### Common Package

Shared between client and server:
- **Config (Singleton)** — Loads `.env` files once, provides key-value access everywhere.
- **Message** — The single unit of communication. Serializable POJO sent over `ObjectStreams`.
- **MessageType** — Enum defining all valid message types in the protocol.
- **ProtocolConstants** — Shared constants (default port, timeouts).

### Communication Flow

```
Client                              Server
  │                                    │
  │── TCP Connect ────────────────────▶│  ServerSocket.accept()
  │                                    │  → new ClientHandler(socket)
  │                                    │  → threadPool.submit(handler)
  │                                    │
  │── CONNECT (secret + username) ───▶│  AuthProxyServer.authenticate()
  │                                    │  → CommandFactory.create(CONNECT)
  │                                    │  → ConnectCommand.execute()
  │◀── ACK "Welcome, Alice!" ─────────│  → registerClient("Alice")
  │                                    │
  │── LIST_ROOMS ────────────────────▶│  → ListRoomsCommand.execute()
  │◀── ROOM_LIST (room details) ──────│
  │                                    │
  │── CREATE_ROOM "general" ─────────▶│  → CreateRoomCommand.execute()
  │◀── ACK "Room created" ────────────│  → roomManager.createRoom()
  │                                    │
  │── JOIN_ROOM "general" ───────────▶│  → JoinRoomCommand.execute()
  │◀── ACK "You joined general" ──────│  → room.addObserver(handler)
  │◀── SYSTEM "Alice joined" ─────────│  → room.notifyObservers()
  │                                    │
  │── CHAT_MESSAGE "Hello!" ─────────▶│  → SendMessageCommand.execute()
  │                                    │  → message.stampTimestamp()
  │◀── CHAT_MESSAGE (broadcast) ──────│  → room.notifyObservers()
  │                                    │  → ALL observers receive it
  │                                    │
  │── LEAVE_ROOM ────────────────────▶│  → LeaveRoomCommand.execute()
  │◀── ACK "You left general" ────────│  → room.removeObserver(handler)
  │                                    │  → room.notify("Alice left")
  │                                    │
  │── DISCONNECT ────────────────────▶│  → DisconnectCommand.execute()
  │◀── ACK "Goodbye!" ────────────────│  → cleanup all state
  │                                    │  → close connection
```

### Threading Model

```
SERVER:
┌─────────────────────────────────────────────────┐
│ Main Thread (ServerDriver.main)                  │
│   └── ChatServer.start() [accept loop]           │
│                                                  │
│ Thread Pool (50 threads):                        │
│   ├── ClientHandler-1 (Alice)  ── read loop      │
│   ├── ClientHandler-2 (Bob)    ── read loop      │
│   └── ClientHandler-3 (Charlie)── read loop      │
│                                                  │
│ Shutdown Hook Thread:                            │
│   └── Triggered on Ctrl+C → server.shutdown()    │
└─────────────────────────────────────────────────┘

CLIENT (per instance):
┌─────────────────────────────────────────────────┐
│ Main Thread (ClientDriver.main)                  │
│   └── MenuHandler.run() [user input loop]        │
│                                                  │
│ Daemon Thread:                                   │
│   └── MessageListener.run() [receive loop]       │
└─────────────────────────────────────────────────┘
```

---

## 📋 Prerequisites

- **Java Development Kit (JDK)** 8 or higher
- **Terminal/Command Prompt** — two or more instances (one for server, one+ for clients)
- **Operating System** — Windows, Linux, or macOS

Verify Java installation:
```bash
java -version
javac -version
```

---

## 🔧 Setup & Installation

### 1. Clone or Download the Project

```bash
git clone <repository-url>
cd project
```

### 2. Verify Folder Structure

Ensure the following structure exists:

```
project/
├── common/
├── server/
│   ├── command/
│   └── observer/
├── client/
└── resources/
    ├── server.env
    └── client.env
```

### 3. Create Configuration Files

If not already present, create the `.env` files:

**`resources/server.env`**
```properties
# Server Configuration
PORT=9000
SECRET=my_super_secret_key_123
MAX_CLIENTS=50
```

**`resources/client.env`**
```properties
# Client Configuration
HOST=localhost
PORT=9000
SECRET=my_super_secret_key_123
```

> ⚠️ **Important:** The `SECRET` value must be **identical** in both `server.env` and `client.env`. This is the shared authentication key.

---

## ⚙️ Configuration

### Server Configuration (`resources/server.env`)

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `PORT` | No | `9000` | Port the server listens on |
| `SECRET` | **Yes** | — | Shared secret for client authentication |
| `MAX_CLIENTS` | No | `50` | Maximum concurrent connections |

### Client Configuration (`resources/client.env`)

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `HOST` | No | `localhost` | Server hostname or IP address |
| `PORT` | No | `9000` | Server port to connect to |
| `SECRET` | **Yes** | — | Must match server's SECRET |

### Connecting to a Remote Server

To connect to a server on another machine, change the client's `HOST`:

```properties
HOST=192.168.1.100
PORT=9000
SECRET=my_super_secret_key_123
```

> Ensure the server machine's firewall allows incoming connections on the configured port.

---

## 🔨 Compilation

### Option 1: Using Compilation Script

**Windows:**
```cmd
compile.bat
```

**Linux/Mac:**
```bash
chmod +x compile.sh
./compile.sh
```

### Option 2: Manual Compilation

```bash
# Create output directory
mkdir -p out

# Compile all source files
javac -d out \
    common/*.java \
    server/observer/*.java \
    server/command/*.java \
    server/*.java \
    client/*.java
```

### Expected Output

```
===== Compiling Chat Application =====
===== Compilation Successful =====

To run the server:
  java -cp out server.ServerDriver

To run a client:
  java -cp out client.ClientDriver
```

> If compilation fails, ensure all files are in the correct packages and the JDK is properly installed.

---

## 🚀 Execution

### Step 1: Start the Server

Open a terminal and run:

```bash
java -cp out server.ServerDriver
```

**Expected Output:**
```
=== Chat Server Starting ===
[Config] Loaded 3 properties from resources/server.env
[AuthProxy] Authentication proxy active.
[AuthProxy] Secret loaded: my_***
========================================
  Chat Server started on port 9000
  Press Ctrl+C to shutdown.
========================================
```

### Step 2: Start Client(s)

Open **one or more** additional terminals and run:

```bash
java -cp out client.ClientDriver
```

**Expected Output:**
```
=== Chat Client Starting ===
[Config] Loaded 3 properties from resources/client.env
Enter your username: Alice
Connecting as 'Alice' to localhost:9000...
[Connection] Connected to localhost:9000
[Server] Welcome, Alice! You are now connected.

╔══════════════════════════════╗
║        MAIN MENU             ║
╠══════════════════════════════╣
║  1. List Chat Rooms          ║
║  2. Create Chat Room         ║
║  3. Join Chat Room           ║
║  4. Delete Chat Room         ║
║  5. Quit                     ║
╚══════════════════════════════╝
Choose an option:
```

### Step 3: Interact

Start a second client in another terminal with a different username (e.g., "Bob"), create a room, join it from both clients, and start chatting!

### Stopping

- **Client:** Choose option `5` (Quit) from the menu, or press `Ctrl+C`.
- **Server:** Press `Ctrl+C` in the server terminal. The shutdown hook will gracefully disconnect all clients and terminate all threads.

---

## 📖 Usage Guide

### Main Menu Options

| Option | Action | Description |
|--------|--------|-------------|
| `1` | List Chat Rooms | Shows all available rooms with creator and member count |
| `2` | Create Chat Room | Creates a new room (you become the owner) |
| `3` | Join Chat Room | Enter a room to start chatting |
| `4` | Delete Chat Room | Delete a room you created (removes all members) |
| `5` | Quit | Disconnect from server and exit |

### Chat Mode Commands

Once inside a chat room:

| Command | Description |
|---------|-------------|
| *(any text)* | Send a message to the room |
| `/leave` | Leave the current room and return to main menu |
| `/help` | Show available chat commands |

### Example Session

```
=== Terminal 1: Alice ===
Choose an option: 2
Enter room name: general
[Server] Room 'general' created successfully.

Choose an option: 3
Enter room name to join: general

╔══════════════════════════════════════╗
║  Joined Room: general                ║
║  Type /leave to exit, /help for help ║
╚══════════════════════════════════════╝
[Server] You joined room 'general'.
*** Alice has joined the room. ***

Hello everyone!
[14:30:15] Alice: Hello everyone!

*** Bob has joined the room. ***

[14:30:45] Bob: Hey Alice!

/leave
[Server] You left room 'general'.

Choose an option: 5
[Server] Goodbye, Alice!
[Client] Goodbye!
```

---

## 🧩 Design Patterns — Detailed Explanation

### 1. Singleton Pattern

**File:** `common/Config.java`

**Intent:** Ensure a class has only one instance and provide a global point of access to it.

**Implementation:**
```java
public class Config {
    private static volatile Config instance = null;
    private final Map<String, String> properties;

    private Config(String filePath) { /* load file */ }

    public static Config getInstance(String filePath) {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config(filePath);
                }
            }
        }
        return instance;
    }
}
```

**How It Works:**
- Private constructor prevents external instantiation.
- Double-checked locking ensures thread-safe lazy initialization.
- `volatile` keyword prevents instruction reordering issues.
- Configuration is loaded once from a `.env` file and made immutable (no setters).

**Why Here:** Configuration should be loaded exactly once and accessible everywhere — both on the server (port, secret) and client (host, port, secret). Multiple instances would waste memory and risk inconsistency.

---

### 2. Proxy Pattern

**Files:** `server/AuthProxyServer.java` wrapping `server/ChatServer.java`

**Intent:** Provide a surrogate or placeholder for another object to control access to it.

**Implementation:**
```java
public class AuthProxyServer {
    private final ChatServer realServer;  // Wrapped object
    private final String secret;

    public boolean authenticate(Message msg) {
        return secret.equals(msg.getSecret());
    }

    public void start() {
        realServer.start();  // Delegates to real server
    }
}
```

**How It Works:**
- `AuthProxyServer` wraps `ChatServer` using **composition** (not inheritance).
- Every client's first message is validated by `authenticate()` before the connection is accepted.
- If the secret doesn't match, the client is rejected immediately.
- After authentication, the `ClientHandler` processes subsequent messages normally.

**Why Here:** Authentication is a cross-cutting concern. The Proxy pattern separates authentication logic from server logic. The `ChatServer` doesn't need to know about secrets — it just handles chat operations. This follows the Single Responsibility Principle.

---

### 3. Observer Pattern

**Files:**
- `server/observer/ChatRoomSubject.java` — Subject interface
- `server/observer/ChatRoomObserver.java` — Observer interface
- `server/ChatRoom.java` — Concrete Subject
- `server/ClientHandler.java` — Concrete Observer

**Intent:** Define a one-to-many dependency between objects so that when one object changes state, all its dependents are notified automatically.

**Implementation:**
```java
// Subject
public class ChatRoom implements ChatRoomSubject {
    private List<ChatRoomObserver> observers = new CopyOnWriteArrayList<>();

    public void notifyObservers(Message msg) {
        for (ChatRoomObserver observer : observers) {
            try {
                observer.onMessage(msg);
            } catch (Exception e) {
                observers.remove(observer);  // Dead client
            }
        }
    }
}

// Observer
public class ClientHandler implements ChatRoomObserver {
    public void onMessage(Message msg) throws IOException {
        sendMessage(msg);  // Push to client's socket
    }
}
```

**How It Works:**
- When a client joins a room, their `ClientHandler` is registered as an observer.
- When any client sends a message, the `ChatRoom` calls `notifyObservers()`.
- Each observer's `onMessage()` pushes the message through the socket to the client.
- If `onMessage()` throws `IOException` (client disconnected), the observer is automatically removed.

**Why Here:** Chat rooms are a textbook Observer scenario — one message must be delivered to all participants. The pattern decouples the sender from the receivers. The `ChatRoom` doesn't know anything about sockets or networking — it just notifies observers.

---

### 4. Command Pattern

**Files:** `server/command/Command.java`, all concrete commands

**Intent:** Encapsulate a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations.

**Implementation:**
```java
// Interface
public interface Command {
    void execute();
}

// Concrete Command
public class JoinRoomCommand implements Command {
    private final Message msg;
    private final ClientHandler client;
    private final ChatServer server;

    public void execute() {
        ChatRoom room = server.getRoomManager().getRoom(msg.getRoomId());
        room.addObserver(client);
        // ... send ACK, notify room
    }
}
```

**How It Works:**
- Each message type maps to a concrete `Command` class.
- The `ClientHandler` doesn't contain any business logic — it just reads messages and delegates to commands.
- Each command encapsulates all the logic for one specific operation.
- Commands receive references to `ClientHandler`, `ChatServer`, and the original `Message`.

**Why Here:** Without the Command pattern, `ClientHandler.run()` would contain a massive switch-case block with all business logic mixed together. The pattern:
- Separates concerns (each command is independent)
- Makes adding new features trivial (add new class + factory line)
- Makes each operation independently testable
- Follows Open/Closed Principle

---

### 5. Factory Method Pattern

**File:** `server/command/CommandFactory.java`

**Intent:** Define an interface for creating an object, but let subclasses (or a factory) decide which class to instantiate.

**Implementation:**
```java
public class CommandFactory {
    public static Command create(Message msg, ClientHandler client,
                                 ChatServer server) {
        switch (msg.getType()) {
            case CONNECT:      return new ConnectCommand(msg, client, server);
            case JOIN_ROOM:    return new JoinRoomCommand(msg, client, server);
            case CHAT_MESSAGE: return new SendMessageCommand(msg, client, server);
            // ... other cases
            default:           return new ErrorCommand(client, "Unknown");
        }
    }
}
```

**How It Works:**
- The factory examines the `MessageType` and creates the correct `Command` object.
- The caller (`ClientHandler`) doesn't know or care which concrete command is created.
- New message types require only a new `Command` class and one new `case` line.

**Why Here:** The factory centralizes object creation, eliminating scattered `new` statements and `if-else` chains throughout the code. It pairs naturally with the Command pattern.

---

### 6. Template Method Pattern

**File:** `server/ClientHandler.java` — `run()` method

**Intent:** Define the skeleton of an algorithm in a method, deferring some steps to subclasses or delegate objects.

**Implementation:**
```java
public void run() {
    // Step 1: Initialize streams (fixed)
    outputStream = new ObjectOutputStream(socket.getOutputStream());
    inputStream = new ObjectInputStream(socket.getInputStream());

    while (running) {
        // Step 2: Read message (fixed)
        Message msg = (Message) inputStream.readObject();

        // Step 3: Authenticate if needed (fixed structure)
        if (!authenticated) {
            if (!authProxy.authenticate(msg)) { disconnect(); return; }
            authenticated = true;
        }

        // Step 4: Create and execute command (variable — depends on message type)
        Command command = CommandFactory.create(msg, this, server);
        command.execute();
    }
}
```

**How It Works:**
- The `run()` method defines a fixed skeleton: initialize → read → authenticate → execute.
- The variable part (what happens for each message) is delegated to `Command` objects.
- The lifecycle is standardized — every client goes through the same steps.

**Why Here:** All client connections follow the same lifecycle. The Template Method ensures consistency while allowing the behavior to vary based on the message type (via Command pattern).

---

### 7. Composition Over Inheritance

**Files:** `AuthProxyServer` uses `ChatServer` via composition

**Principle:** Favor object composition over class inheritance.

**Implementation:**
```java
// CORRECT: Composition
public class AuthProxyServer {
    private final ChatServer realServer;  // Has-a relationship

    public void start() {
        realServer.start();  // Delegates
    }
}

// WRONG: Would have been inheritance
// public class AuthProxyServer extends ChatServer { ... }
```

**Why Composition:**
- `AuthProxyServer` is NOT a type of server — it's a wrapper around one.
- Inheritance would tightly couple the two classes and make testing harder.
- Composition allows swapping the real server implementation without changing the proxy.
- Follows the Liskov Substitution Principle — a proxy shouldn't be substitutable for a server.

---

## 📡 Protocol Specification

### Message Structure

| Field | Type | Description | Set By |
|-------|------|-------------|--------|
| `type` | `MessageType` | Operation type | Client/Server |
| `secret` | `String` | Authentication key | Client |
| `sender` | `String` | Username | Client |
| `roomId` | `String` | Target room ID | Client/Server |
| `content` | `String` | Message body or payload | Client/Server |
| `timestamp` | `long` | Unix timestamp (ms) | **Server only** |

### Message Types

| Type | Direction | Description |
|------|-----------|-------------|
| `CONNECT` | Client → Server | Initial connection with username |
| `DISCONNECT` | Client → Server | Graceful disconnection |
| `LIST_ROOMS` | Client → Server | Request room list |
| `CREATE_ROOM` | Client → Server | Create a new room |
| `DELETE_ROOM` | Client → Server | Delete a room (creator only) |
| `JOIN_ROOM` | Client → Server | Join a chat room |
| `LEAVE_ROOM` | Client → Server | Leave current room |
| `CHAT_MESSAGE` | Bidirectional | Chat message |
| `ACK` | Server → Client | Success acknowledgment |
| `ERROR` | Server → Client | Error response |
| `SYSTEM_MESSAGE` | Server → Client | System notification |
| `ROOM_LIST` | Server → Client | Room listing response |

---

## 🔒 Thread Safety Measures

| Mechanism | Where Used | Why |
|-----------|-----------|-----|
| `ConcurrentHashMap` | `ChatServer.clients`, `ChatRoomManager.rooms` | Thread-safe map operations without external sync |
| `CopyOnWriteArrayList` | `ChatRoom.observers` | Safe iteration during broadcast while others add/remove |
| `AtomicBoolean` | `ChatServer.running`, `ChatClient.running/inChatMode` | Lock-free thread-safe boolean flags |
| `synchronized` method | `ClientHandler.sendMessage()` | Prevents interleaved writes to ObjectOutputStream |
| `volatile` field | `Config.instance`, `ClientHandler.running` | Ensures visibility across threads |
| `ObjectOutputStream.reset()` | After every write | Prevents stale cached object references |
| Output-before-Input | Stream initialization | Prevents deadlock from mutual header waiting |
| Daemon threads | `MessageListener` | Auto-terminates when main thread exits |
| `ExecutorService` | `ChatServer.threadPool` | Bounded thread pool prevents resource exhaustion |
| `finally` blocks | `ClientHandler.run()` | Guarantees cleanup regardless of exit path |

---

## ⚠️ Error Handling

| Scenario | Handling |
|----------|----------|
| Config file missing | Server/Client prints error and exits with code 1 |
| SECRET not configured | Server/Client prints error and exits |
| Wrong secret from client | `AuthProxyServer` rejects → ERROR message → disconnect |
| Duplicate username | `ConnectCommand` sends ERROR, connection stays open for retry |
| Room not found | Command sends ERROR to client |
| Delete by non-creator | `DeleteRoomCommand` sends ERROR |
| Client crashes/disconnects | `IOException` caught → `cleanup()` in `finally` → removed from all rooms |
| Server crashes | Client's `MessageListener` detects → `onConnectionLost()` → client exits |
| Send failure | `sendMessage()` catches `IOException` → sets `running = false` |
| Invalid message type | `CommandFactory` returns `ErrorCommand` |
| `Ctrl+C` on server | Shutdown hook → close server socket → disconnect all clients → shutdown pool |

---

## 🔮 Limitations & Future Improvements

### Current Limitations

- **Console-based UI** — No graphical interface
- **No message persistence** — Messages are lost when the server restarts
- **No private messaging** — All messages are room-based broadcasts
- **Single server** — No clustering or load balancing
- **No encryption** — Messages are sent as serialized Java objects (not encrypted)
- **Java serialization** — Uses `ObjectOutputStream` which is Java-only (not interoperable)

### Potential Improvements

| Improvement | Description |
|-------------|-------------|
| **GUI Client** | Swing/JavaFX frontend — only `MenuHandler` needs replacement |
| **Database Persistence** | Store messages and rooms in SQLite/MySQL |
| **Private Messages** | Add `PRIVATE_MESSAGE` type with target username |
| **JSON Protocol** | Replace Java serialization with JSON for cross-language support |
| **TLS/SSL** | Wrap sockets in `SSLSocket` for encrypted communication |
| **File Sharing** | Add `FILE_TRANSFER` message type with binary payload |
| **User Authentication** | Replace shared secret with per-user credentials |
| **Message History** | Send last N messages when a user joins a room |
| **Admin Commands** | Kick users, ban users, promote moderators |
| **Rate Limiting** | Prevent spam with per-client message rate limits |

---

## 👥 Contributors

| Name | Role |
|------|------|
| *Your Name* | Developer |
| *Partner Name (if any)* | Developer |

---

## 📄 License

This project was developed as a **Design Patterns** course project for academic purposes.

---

*Built with ☕ Java and 7 Design Patterns*
```