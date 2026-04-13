## Design Patterns Used (7 Total)

| # | Pattern | Where | Why |
|---|---------|-------|-----|
| 1 | **Singleton** | Config | Single source of configuration |
| 2 | **Observer** | ChatRoom ↔ ClientHandler | Real-time message broadcast |
| 3 | **Command** | Request handling | Decouple request parsing from execution, extensible |
| 4 | **Factory Method** | CommandFactory | Create correct Command from request type |
| 5 | **Proxy** | AuthProxy wrapping Server | Gate access with secret validation before forwarding |
| 6 | **Strategy** | Message formatting (optional but clean) | Different formatting for system vs user messages |
| 7 | **Template Method** | Base connection setup in abstract classes | Shared connection lifecycle, customizable steps |

---

## Refined Project Structure

```
project/
├── common/                          # Shared between client & server
│   ├── Config.java
│   ├── Message.java
│   ├── MessageType.java
│   └── ProtocolConstants.java
│
├── server/
│   ├── ServerDriver.java
│   ├── ChatServer.java
│   ├── AuthProxyServer.java
│   ├── ClientHandler.java
│   ├── ChatRoom.java
│   ├── ChatRoomManager.java
│   ├── command/
│   │   ├── Command.java
│   │   ├── CommandFactory.java
│   │   ├── ConnectCommand.java
│   │   ├── DisconnectCommand.java
│   │   ├── JoinRoomCommand.java
│   │   ├── LeaveRoomCommand.java
│   │   ├── CreateRoomCommand.java
│   │   ├── DeleteRoomCommand.java
│   │   ├── ListRoomsCommand.java
│   │   └── SendMessageCommand.java
│   └── observer/
│       ├── ChatRoomObserver.java
│       └── ChatRoomSubject.java
│
├── client/
│   ├── ClientDriver.java
│   ├── ChatClient.java
│   ├── ServerConnection.java
│   ├── MessageListener.java
│   └── MenuHandler.java
│
└── resources/
    ├── server.env
    └── client.env
```

---

## COMMON PACKAGE

### `MessageType.java` (Enum)
```
Defines all request/response types in one place.
```
```
CONNECT, DISCONNECT,
LIST_ROOMS, CREATE_ROOM, DELETE_ROOM,
JOIN_ROOM, LEAVE_ROOM,
CHAT_MESSAGE,
ACK, ERROR,
SYSTEM_MESSAGE
```

**Justification:** Centralizing message types prevents magic strings scattered across both client and server. Both sides share this contract.

---

### `Message.java` (Serializable POJO)
```
The single unit of communication over sockets. Serialized to JSON or ObjectStream.
```

| Field | Type | Purpose |
|-------|------|---------|
| `type` | `MessageType` | What kind of message |
| `secret` | `String` | Auth token for proxy validation |
| `sender` | `String` | Username of sender |
| `roomId` | `String` | Target chat room |
| `content` | `String` | Actual text or payload |
| `timestamp` | `long` | Unix timestamp |

| Method | Description |
|--------|-------------|
| `toJson(): String` | Serialize to JSON string |
| `fromJson(String): Message` | Static factory — deserialize |
| `validate(): boolean` | Check required fields present based on type |

**Justification:** A unified message object replaces scattered string parsing. Both sides speak the same language. `validate()` catches malformed messages early.

---

### `Config.java` (Singleton)

| Method | Description |
|--------|-------------|
| `static getInstance(String filePath): Config` | Lazy init, loads .env file on first call |
| `get(String key): String` | Returns value or empty string `""` |
| `getInt(String key): int` | Convenience — parsed integer |
| `has(String key): boolean` | Check if key exists |

| Responsibility |
|----------------|
| Load key-value pairs from `.env` file once |
| Thread-safe lazy initialization (double-checked locking) |
| Immutable after loading — no setters |

**Justification:** Your original idea was correct. I added `getInt()` and `has()` for convenience, and made it immutable after load to prevent runtime config corruption.

---

### `ProtocolConstants.java`
```
Static final fields: TIMEOUT_MS, MAX_MESSAGE_SIZE, ACK_WAIT_MS, MAX_RETRIES
```

**Justification:** Avoids magic numbers. Both client and server reference the same constants.

---

## SERVER PACKAGE

### `ServerDriver.java`

| Method | Description |
|--------|-------------|
| `main(String[] args)` | Entry point |
| `registerShutdownHook(AuthProxyServer)` | Registers Runtime shutdown hook for Ctrl+C |

| Responsibility |
|----------------|
| Load Config — exit with error if `server.env` missing |
| Instantiate `AuthProxyServer` wrapping `ChatServer` |
| Start server |
| Register JVM shutdown hook → calls `server.shutdown()` |

**Justification:** Your original driver idea refined. Java's `Runtime.addShutdownHook()` is the correct mechanism for Ctrl+C handling — not manual signal trapping.

---

### `ChatServer.java`

| Field | Type | Purpose |
|-------|------|---------|
| `serverSocket` | `ServerSocket` | Listens on configured port |
| `clientHandlers` | `ConcurrentHashMap<String, ClientHandler>` | Active clients by username |
| `roomManager` | `ChatRoomManager` | Manages all chat rooms |
| `threadPool` | `ExecutorService` | Thread pool for client handlers |
| `running` | `AtomicBoolean` | Server state flag |

| Method | Signature | Description |
|--------|-----------|-------------|
| `start` | `void start()` | Opens socket, enters accept loop, submits each client to threadPool |
| `shutdown` | `void shutdown()` | Sets running=false, closes all ClientHandlers, shuts down threadPool, closes serverSocket |
| `registerClient` | `void registerClient(String username, ClientHandler handler)` | Add to map |
| `removeClient` | `void removeClient(String username)` | Remove from map + remove from all rooms |
| `getRoomManager` | `ChatRoomManager getRoomManager()` | Getter |
| `isUsernameTaken` | `boolean isUsernameTaken(String name)` | Check map |

**Key design change from your original:**
> You wanted thread-per-request. This is **thread-per-connection** using a **fixed thread pool** (`Executors.newFixedThreadPool`).

**Justification:** A chat client must stay connected to receive messages. Thread-per-request would mean the client connects, gets a response, disconnects — that's HTTP, not chat. Thread-per-connection keeps the socket alive. The thread pool prevents unbounded thread creation (resource exhaustion attack vector).

---

### `AuthProxyServer.java` — **Proxy Pattern**

| Field | Type | Purpose |
|-------|------|---------|
| `realServer` | `ChatServer` | The wrapped server |
| `secret` | `String` | From config |

| Method | Signature | Description |
|--------|-----------|-------------|
| `start` | `void start()` | Delegates to `realServer.start()` — but injects itself as the authenticator |
| `shutdown` | `void shutdown()` | Delegates to `realServer.shutdown()` |
| `authenticate` | `boolean authenticate(Message msg)` | Checks `msg.getSecret().equals(this.secret)` |

**How it integrates:** `ClientHandler` holds a reference to `AuthProxyServer`. Every incoming message passes through `authenticate()` before being processed. First message (CONNECT) must authenticate — subsequent messages on the same connection are already trusted (session-based).

**Justification (vs your original):** You had ProxyServer extending Server. That creates an inheritance problem — the proxy IS NOT a different kind of server, it wraps one. Composition over inheritance. The proxy validates the **first message's secret**, then marks the connection as authenticated. This avoids checking the secret on every single chat message (performance).

---

### `ClientHandler.java` (implements `Runnable`)

| Field | Type | Purpose |
|-------|------|---------|
| `socket` | `Socket` | Client's connection |
| `inputStream` | `ObjectInputStream` | Receive messages |
| `outputStream` | `ObjectOutputStream` | Send messages |
| `username` | `String` | Identified after CONNECT |
| `authenticated` | `boolean` | Set after first message validated |
| `server` | `ChatServer` | Back-reference |
| `authProxy` | `AuthProxyServer` | For authentication |
| `currentRoom` | `String` | Currently joined room (or null) |

| Method | Signature | Description |
|--------|-----------|-------------|
| `run` | `void run()` | Main loop: read message → validate → create Command → execute |
| `sendMessage` | `void sendMessage(Message msg)` | Synchronized write to output stream |
| `disconnect` | `void disconnect()` | Close streams, close socket, remove from server |
| `getUsername` | `String getUsername()` | Getter |
| `getCurrentRoom` | `String getCurrentRoom()` | Getter |
| `setCurrentRoom` | `void setCurrentRoom(String)` | Setter |

**`run()` pseudocode:**
```
while (running):
    message = inputStream.readObject()
    if (!authenticated):
        if (authProxy.authenticate(message) && message.type == CONNECT):
            authenticated = true
            // register
        else:
            sendMessage(ERROR)
            disconnect()
            return
    else:
        Command cmd = CommandFactory.create(message, this, server)
        cmd.execute()
```

**Justification:** This is the heart of the server. One thread per client, reading in a loop. The Command pattern decouples "what to do" from "how the message arrived."

---

### `ChatRoomManager.java`

| Field | Type |
|-------|------|
| `rooms` | `ConcurrentHashMap<String, ChatRoom>` |

| Method | Signature | Description |
|--------|-----------|-------------|
| `createRoom` | `ChatRoom createRoom(String id, String creator)` | Create and store new room |
| `deleteRoom` | `boolean deleteRoom(String id, String requester)` | Only creator can delete; notifies all observers, removes them |
| `getRoom` | `ChatRoom getRoom(String id)` | Get by ID or null |
| `listRooms` | `List<String> listRooms()` | Returns list of room IDs with member counts |
| `removeClientFromAll` | `void removeClientFromAll(ClientHandler handler)` | Called on disconnect — cleanup |

**Justification:** Extracted from ChatServer to follow Single Responsibility. The server manages connections; the manager manages rooms. This also makes it testable in isolation.

---

### `ChatRoom.java` — **Observer Pattern (Subject)**

Implements `ChatRoomSubject`

| Field | Type | Purpose |
|-------|------|---------|
| `roomId` | `String` | Unique ID |
| `creator` | `String` | Username who created it |
| `observers` | `CopyOnWriteArrayList<ChatRoomObserver>` | Thread-safe observer list |

| Method | Signature | Description |
|--------|-----------|-------------|
| `addObserver` | `void addObserver(ChatRoomObserver o)` | Add client to room |
| `removeObserver` | `void removeObserver(ChatRoomObserver o)` | Remove client |
| `notifyObservers` | `void notifyObservers(Message msg)` | Broadcast to all; if send fails, remove that observer |
| `getObserverCount` | `int getObserverCount()` | For listing |
| `hasObserver` | `boolean hasObserver(ChatRoomObserver o)` | Check membership |
| `getRoomId` | `String getRoomId()` | Getter |
| `getCreator` | `String getCreator()` | Getter |

**Key refinement from your original:**
> You mentioned "if ACK doesn't come, remove client." This is the right instinct but ACK-based removal adds enormous complexity (timeouts, retry queues, async waiting). Instead: **if `sendMessage()` throws an IOException, the client is dead — remove immediately.** This is simpler and equally reliable.

```java
void notifyObservers(Message msg) {
    for (ChatRoomObserver o : observers) {
        try {
            o.onMessage(msg);
        } catch (Exception e) {
            removeObserver(o);  // Dead client
        }
    }
}
```

**Justification:** `CopyOnWriteArrayList` is perfect here — reads (broadcasts) vastly outnumber writes (join/leave). IOException-based removal is simpler than ACK tracking and catches the same failure condition (dead connection).

---

### `ChatRoomObserver.java` (Interface)
```java
public interface ChatRoomObserver {
    void onMessage(Message msg) throws IOException;
}
```

### `ChatRoomSubject.java` (Interface)
```java
public interface ChatRoomSubject {
    void addObserver(ChatRoomObserver o);
    void removeObserver(ChatRoomObserver o);
    void notifyObservers(Message msg);
}
```

`ClientHandler` implements `ChatRoomObserver`:
```java
@Override
public void onMessage(Message msg) throws IOException {
    sendMessage(msg);  // If this fails, IOException propagates up
}
```

---

### Command Package — **Command + Factory Patterns**

#### `Command.java` (Interface)
```java
public interface Command {
    void execute();
}
```

#### `CommandFactory.java`
```java
public class CommandFactory {
    public static Command create(Message msg, ClientHandler client, ChatServer server) {
        switch (msg.getType()) {
            case LIST_ROOMS:    return new ListRoomsCommand(client, server);
            case CREATE_ROOM:   return new CreateRoomCommand(msg, client, server);
            case DELETE_ROOM:   return new DeleteRoomCommand(msg, client, server);
            case JOIN_ROOM:     return new JoinRoomCommand(msg, client, server);
            case LEAVE_ROOM:    return new LeaveRoomCommand(msg, client, server);
            case CHAT_MESSAGE:  return new SendMessageCommand(msg, client, server);
            case DISCONNECT:    return new DisconnectCommand(client, server);
            default:            return () -> client.sendMessage(errorMsg("Unknown command"));
        }
    }
}
```

**Justification for Command pattern:** Your original design had the server checking request type with what would inevitably become a giant if-else/switch block mixing parsing with logic. Command pattern:
- Each command is a **separate class** with **single responsibility**
- Adding new commands = adding a new class + one factory line (Open/Closed Principle)
- Commands are **testable in isolation**
- The factory centralizes object creation

#### Individual Commands (all implement `Command`):

| Command | `execute()` behavior |
|---------|---------------------|
| `ConnectCommand` | Validate username not taken → register client → send ACK with room list |
| `ListRoomsCommand` | Get rooms from manager → send list to client |
| `CreateRoomCommand` | Create room via manager → send ACK |
| `DeleteRoomCommand` | Validate creator → notify members → delete → send ACK |
| `JoinRoomCommand` | Get room → add client as observer → broadcast "X joined" → send ACK |
| `LeaveRoomCommand` | Get room → remove observer → broadcast "X left" → send ACK |
| `SendMessageCommand` | Validate client is in a room → room.notifyObservers(msg) |
| `DisconnectCommand` | Remove from all rooms → unregister → close connection |

Each command holds `ClientHandler`, `ChatServer`, and optionally `Message` as constructor parameters.

---

## CLIENT PACKAGE

### `ClientDriver.java`

| Method | Description |
|--------|-------------|
| `main(String[] args)` | Entry point |

| Responsibility |
|----------------|
| Load Config — exit if `client.env` missing |
| Prompt user for username (validate non-empty) |
| Store username in Config or pass to ChatClient |
| Create `ChatClient` → call `start()` |

---

### `ServerConnection.java` — **Template Method Pattern**

Handles the raw socket lifecycle.

| Field | Type |
|-------|------|
| `socket` | `Socket` |
| `outputStream` | `ObjectOutputStream` |
| `inputStream` | `ObjectInputStream` |
| `connected` | `AtomicBoolean` |

| Method | Signature | Description |
|--------|-----------|-------------|
| `connect` | `void connect(String host, int port)` | Open socket + streams |
| `send` | `void send(Message msg)` | Synchronized write |
| `receive` | `Message receive()` | Blocking read |
| `disconnect` | `void disconnect()` | Close everything |
| `isConnected` | `boolean isConnected()` | Getter |

**Justification:** Extracted from ChatClient so that network I/O is isolated. ChatClient focuses on business logic, ServerConnection focuses on transport. This follows Single Responsibility and makes it possible to swap transport (e.g., test with mock connection).

---

### `MessageListener.java` (implements `Runnable`)

Runs on a **separate daemon thread**, continuously reading incoming messages.

| Field | Type |
|-------|------|
| `connection` | `ServerConnection` |
| `chatClient` | `ChatClient` |

| Method | Signature | Description |
|--------|-----------|-------------|
| `run` | `void run()` | Loop: `message = connection.receive()` → `chatClient.handleIncoming(message)` |

**Justification:** The client needs to **simultaneously** wait for user input (menu) AND listen for incoming messages (other users chatting). Without a listener thread, the client would block on reading user input and miss server messages, or vice versa. This is the standard pattern for chat clients.

---

### `ChatClient.java`

This is the **main client controller**.

| Field | Type |
|-------|------|
| `username` | `String` |
| `connection` | `ServerConnection` |
| `listener` | `MessageListener` |
| `menuHandler` | `MenuHandler` |
| `currentRoom` | `String` |
| `inChatMode` | `AtomicBoolean` |

| Method | Signature | Description |
|--------|-----------|-------------|
| `start` | `void start()` | Connect to server, send CONNECT message with secret, start listener thread, launch menu |
| `handleIncoming` | `void handleIncoming(Message msg)` | Route incoming message by type: display chat, show error, show system msg |
| `joinRoom` | `void joinRoom(String roomId)` | Send JOIN_ROOM message |
| `leaveRoom` | `void leaveRoom()` | Send LEAVE_ROOM message |
| `createRoom` | `void createRoom(String roomId)` | Send CREATE_ROOM message |
| `deleteRoom` | `void deleteRoom(String roomId)` | Send DELETE_ROOM message |
| `listRooms` | `void listRooms()` | Send LIST_ROOMS message |
| `sendChat` | `void sendChat(String text)` | Send CHAT_MESSAGE |
| `disconnect` | `void disconnect()` | Send DISCONNECT, close connection |

---

### `MenuHandler.java`

Handles all console I/O and menu display.

| Method | Signature | Description |
|--------|-----------|-------------|
| `showMainMenu` | `void showMainMenu()` | Display options: List/Create/Join/Delete/Quit |
| `showChatMode` | `void showChatMode()` | Display chat prompt, type messages, `/leave` to exit |
| `run` | `void run(ChatClient client)` | Main input loop — reads choice, delegates to ChatClient methods |
| `displayMessage` | `void displayMessage(Message msg)` | Format and print to console (thread-safe with synchronized System.out) |

**Justification:** Separating UI from client logic means you could replace console with GUI later without touching `ChatClient`. Also, `displayMessage` being thread-safe is critical because the listener thread and the menu thread both print to console.

---

## Communication Flow

```
Client                          Server
  │                                │
  │──── CONNECT (secret, user) ──→│ AuthProxy validates secret
  │←──── ACK / ERROR ─────────────│ ClientHandler registered
  │                                │
  │──── LIST_ROOMS ──────────────→│ CommandFactory → ListRoomsCommand
  │←──── SYSTEM_MESSAGE (list) ───│
  │                                │
  │──── JOIN_ROOM (roomId) ──────→│ CommandFactory → JoinRoomCommand
  │←──── ACK ─────────────────────│ Added as observer
  │←──── SYSTEM_MSG ("X joined") ─│ Broadcast to room
  │                                │
  │──── CHAT_MESSAGE (text) ─────→│ CommandFactory → SendMessageCommand
  │←──── CHAT_MESSAGE (from Y) ───│ room.notifyObservers()
  │                                │
  │──── LEAVE_ROOM ──────────────→│ CommandFactory → LeaveRoomCommand
  │←──── ACK ─────────────────────│ Removed as observer
  │                                │
  │──── DISCONNECT ──────────────→│ CommandFactory → DisconnectCommand
  │                                │ Cleanup everything
```

---

## Class Diagram Relationships (for your UML)

```
┌─────────────────────────────────────────────────┐
│                    COMMON                        │
│  Config (Singleton)                              │
│  Message (Serializable)                          │
│  MessageType (Enum)                              │
│  ProtocolConstants                               │
└─────────────────────────────────────────────────┘

┌─────────────────── SERVER ──────────────────────┐
│                                                  │
│  ServerDriver                                    │
│      │ creates                                   │
│      ▼                                           │
│  AuthProxyServer ──────wraps────→ ChatServer     │
│  (Proxy Pattern)                   │  has-a      │
│                                    ▼             │
│                              ChatRoomManager     │
│                                    │  has-many   │
│                                    ▼             │
│  ClientHandler ◄──observes──── ChatRoom          │
│  (ChatRoomObserver)           (ChatRoomSubject)  │
│      │                                           │
│      │ delegates to                              │
│      ▼                                           │
│  CommandFactory ──creates──→ «interface» Command │
│  (Factory Pattern)           ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲   │
│                              │ │ │ │ │ │ │ │    │
│            ConnectCmd, JoinRoomCmd, SendMsgCmd... │
└──────────────────────────────────────────────────┘

┌─────────────────── CLIENT ──────────────────────┐
│                                                  │
│  ClientDriver                                    │
│      │ creates                                   │
│      ▼                                           │
│  ChatClient ────has-a────→ ServerConnection      │
│      │                                           │
│      ├──has-a────→ MessageListener (Thread)      │
│      │                                           │
│      └──has-a────→ MenuHandler                   │
└──────────────────────────────────────────────────┘
```

---

## Summary of Changes from Your Original

| Your Idea | Refined Version | Why |
|-----------|----------------|-----|
| Thread ends after request | Thread-per-connection (persistent) | Chat requires persistent connections for real-time delivery |
| ProxyServer extends Server | AuthProxyServer wraps ChatServer (composition) | Correct Proxy pattern; composition > inheritance |
| ACK-based dead client removal | IOException-based removal in `notifyObservers` | Simpler, equally reliable, no timeout complexity |
| Server checks request type (big switch) | Command + Factory pattern | Extensible, testable, follows Open/Closed Principle |
| ChatClient as observer | ClientHandler implements ChatRoomObserver on server side | Observer runs server-side; client just receives pushed messages |
| Single client class | ChatClient + ServerConnection + MessageListener + MenuHandler | Separation of concerns; concurrent input + listening |
| GroupChat class | ChatRoom + ChatRoomManager | Manager extracted for SRP; ChatRoom is a clean Subject |
| No message structure | Unified Message class shared by both | Type-safe, validated, serializable protocol |