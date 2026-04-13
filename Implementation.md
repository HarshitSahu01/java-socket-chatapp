## Design Patterns Used (5 Total)

| # | Pattern | Where | Why |
|---|---------|-------|-----|
| 1 | **Singleton** | Config | Single source of configuration |
| 2 | **Observer** | ChatRoom ↔ ClientHandler | Real-time message broadcast |
| 3 | **Proxy** | AuthProxyServer wrapping ChatServer | Gate access with secret validation |
| 4 | **Composition over Inheritance** | AuthProxyServer wraps ChatServer | Correct proxy without inheritance |
| 5 | **Template Method** | ClientHandler.run() | Shared connection lifecycle |

> The Command and Factory patterns were removed. Message handling is now a straightforward switch block inside `ClientHandler.handleMessage()`, which is simpler and easier to follow.

---

## Project Structure

```
project/
├── common/
│   ├── Config.java              # Singleton — loads .env config
│   ├── Message.java             # Serializable POJO for all communication
│   ├── MessageType.java         # Enum — all message types
│   └── ProtocolConstants.java   # Shared constants (port, host, etc.)
│
├── server/
│   ├── ServerDriver.java        # Entry point — loads config, starts server
│   ├── ChatServer.java          # Accepts connections, manages client map
│   ├── AuthProxyServer.java     # Wraps ChatServer, validates secret on connect
│   ├── ClientHandler.java       # One thread per client — handles all messages
│   ├── ChatRoom.java            # Holds list of connected clients, broadcasts messages
│   └── ChatRoomManager.java     # CRUD for rooms, disconnect cleanup
│
├── client/
│   ├── ClientDriver.java        # Entry point — loads config, prompts username
│   ├── ChatClient.java          # Controller — sends requests, handles responses
│   ├── ServerConnection.java    # Manages TCP socket and object streams
│   ├── MessageListener.java     # Daemon thread — reads messages from server
│   └── MenuHandler.java         # Console UI — menus and input handling
│
└── resources/
    ├── server.env
    └── client.env
```

---

## COMMON PACKAGE

### `MessageType.java`
Enum of all message types. Both client and server share this contract.

```
CONNECT, DISCONNECT,
LIST_ROOMS, CREATE_ROOM, DELETE_ROOM,
JOIN_ROOM, LEAVE_ROOM,
CHAT_MESSAGE,
ACK, ERROR, SYSTEM_MESSAGE, ROOM_LIST
```

---

### `Message.java`
Serializable POJO — the single unit of communication over sockets.

| Field | Type | Purpose |
|-------|------|---------|
| `type` | `MessageType` | What kind of message |
| `secret` | `String` | Auth token (CONNECT only) |
| `sender` | `String` | Username of sender |
| `roomId` | `String` | Target chat room |
| `content` | `String` | Text payload |
| `timestamp` | `long` | Set by server via `stampTimestamp()` |

---

### `Config.java`
Singleton — loads a `.env` file once. Thread-safe with double-checked locking. Immutable after loading.

| Method | Description |
|--------|-------------|
| `getInstance(filePath)` | First-call initializer |
| `getInstance()` | Subsequent calls |
| `get(key)` | Returns value or `""` |
| `getInt(key, default)` | Returns int or default |
| `has(key)` | Check if key exists |

---

### `ProtocolConstants.java`
Static constants shared by both sides — `DEFAULT_PORT`, `DEFAULT_HOST`, `SOCKET_TIMEOUT_MS`, `MAX_MESSAGE_SIZE`.

---

## SERVER PACKAGE

### `ServerDriver.java`
Entry point. Loads config, validates `SECRET`, creates `ChatServer` + `AuthProxyServer`, registers JVM shutdown hook, starts server.

---

### `ChatServer.java`
Opens the `ServerSocket`, accepts connections, submits each to a **fixed thread pool** (50 threads max). Maintains a `ConcurrentHashMap` of active clients by username.

---

### `AuthProxyServer.java` — Proxy Pattern
Wraps `ChatServer`. Validates the `secret` field on the first (CONNECT) message before allowing the client through. Delegates `start()` and `shutdown()` to the real server.

---

### `ClientHandler.java`
One instance per connected client, runs on its own thread.

**run() flow:**
1. Open object streams (output first to avoid deadlock)
2. Loop: read `Message` from client
3. If not authenticated: validate secret → must be CONNECT
4. Call `handleMessage(msg)` — dispatches via switch
5. On IOException: cleanup and exit

**handleMessage() switch cases:**

| Type | Action |
|------|--------|
| `CONNECT` | Validate & register username → send ACK |
| `LIST_ROOMS` | Get room list string → send ROOM_LIST |
| `CREATE_ROOM` | Create room via manager → send ACK |
| `JOIN_ROOM` | Leave old room if any → join new → broadcast join notice |
| `LEAVE_ROOM` | Remove from room → broadcast leave notice → send ACK |
| `CHAT_MESSAGE` | Stamp timestamp → broadcast to room |
| `DELETE_ROOM` | Delegate to manager (creator check inside) → send ACK |
| `DISCONNECT` | Cleanup all rooms → unregister → send goodbye ACK |

**sendMessage()** is synchronized to prevent concurrent writes from the room broadcast thread and direct response.

---

### `ChatRoom.java` — Observer Pattern
Holds a `CopyOnWriteArrayList<ClientHandler>`. On `notifyObservers()`, calls `sendMessage()` on each client.

`notifyAndClear(reason)` — sends a deletion notice and removes all clients (used when a room is deleted).

---

### `ChatRoomManager.java`
Thread-safe room CRUD using `ConcurrentHashMap`.

| Method | Description |
|--------|-------------|
| `createRoom(id, creator)` | Returns new room or null if name taken |
| `deleteRoom(id, requester)` | Only creator can delete; notifies all members |
| `getRoom(id)` | Lookup by ID |
| `listRooms()` | Formatted string of all rooms |
| `removeClientFromAllRooms(client)` | Called on disconnect — notifies remaining members |

---

## CLIENT PACKAGE

### `ClientDriver.java`
Entry point. Loads config, prompts for username, creates and starts `ChatClient`.

---

### `ServerConnection.java`
Manages the TCP socket and object streams. `send()` is synchronized. Streams created with output first to avoid deadlock.

---

### `MessageListener.java`
Daemon thread. Loops calling `connection.receive()` and passes each message to `ChatClient.handleIncoming()`. On IOException, notifies client of connection loss.

---

### `ChatClient.java`
Main controller. Connects to server, sends CONNECT, starts listener thread, launches `MenuHandler`.

`handleIncoming(msg)` routes by type:
- `CHAT_MESSAGE` → print `[HH:mm:ss] user: text`
- `SYSTEM_MESSAGE` → print `*** text ***`; if room deleted, exit chat mode
- `ROOM_LIST` → print room list
- `ACK` → print; if "You joined" ACK with roomId, enter chat mode; if "You left" ACK, exit chat mode
- `ERROR` → print error

---

### `MenuHandler.java`
Console I/O — menus, prompts, and `displayMessage()` (synchronized for thread safety between listener and menu threads).

---

## Communication Flow

```
Client                          Server
  │                                │
  │──── CONNECT (secret, user) ──→│ AuthProxy validates secret
  │←──── ACK / ERROR ─────────────│ ClientHandler registered
  │                                │
  │──── LIST_ROOMS ──────────────→│ ClientHandler switch → ROOM_LIST
  │←──── ROOM_LIST ───────────────│
  │                                │
  │──── JOIN_ROOM (roomId) ──────→│ ClientHandler switch → add to ChatRoom
  │←──── ACK ─────────────────────│
  │←──── SYSTEM_MSG ("X joined") ─│ Broadcast to room
  │                                │
  │──── CHAT_MESSAGE (text) ─────→│ ClientHandler switch → notifyObservers
  │←──── CHAT_MESSAGE (from Y) ───│ ChatRoom broadcasts to all
  │                                │
  │──── LEAVE_ROOM ──────────────→│ ClientHandler switch → remove from room
  │←──── ACK ─────────────────────│
  │                                │
  │──── DISCONNECT ──────────────→│ Cleanup + close
```

---

## Thread Safety Summary

| Mechanism | Purpose |
|-----------|---------|
| `ConcurrentHashMap` | Thread-safe client/room maps |
| `CopyOnWriteArrayList` | Safe broadcast iteration |
| `AtomicBoolean` | Lock-free running/mode flags |
| `synchronized sendMessage()` | Prevents interleaved socket writes |
| `ObjectOutputStream.reset()` | Prevents stale cached objects |
| Output stream created before input | Prevents initialization deadlock |
| Daemon listener thread | Auto-terminates when client exits |
| `finally` cleanup blocks | Guarantees resource release |