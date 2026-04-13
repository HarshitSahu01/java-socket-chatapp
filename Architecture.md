

# Architecture & Class Diagrams in Mermaid

---

## 1. System Architecture Diagram

```mermaid
graph TB
    subgraph CLIENT_SIDE["🖥️ CLIENT SIDE"]
        CD[ClientDriver]
        CC[ChatClient]
        SC[ServerConnection]
        ML[MessageListener]
        MH[MenuHandler]
        CE[client.env]

        CD -->|creates| CC
        CC -->|has-a| SC
        CC -->|has-a| ML
        CC -->|has-a| MH
        CD -->|loads| CFG_C[Config Singleton]
        CFG_C -->|reads| CE
        ML -->|receives messages| SC
        ML -->|delegates to| CC
        MH -->|user actions| CC
        CC -->|sends messages| SC
    end

    subgraph COMMON["📦 COMMON SHARED PACKAGE"]
        MSG[Message]
        MT[MessageType]
        PC[ProtocolConstants]
        CFG[Config]
    end

    subgraph SERVER_SIDE["🗄️ SERVER SIDE"]
        SD[ServerDriver]
        AP[AuthProxyServer]
        CS[ChatServer]
        CH[ClientHandler]
        CRM[ChatRoomManager]
        CR[ChatRoom]
        CMF[CommandFactory]
        CMD[Commands]
        SE[server.env]

        SD -->|creates| AP
        AP -->|wraps| CS
        SD -->|loads| CFG_S[Config Singleton]
        CFG_S -->|reads| SE
        CS -->|spawns per client| CH
        CS -->|has-a| CRM
        CRM -->|manages many| CR
        CH -->|delegates to| CMF
        CMF -->|creates| CMD
        CMD -->|modifies| CRM
        CMD -->|modifies| CR
        CR -->|notifies| CH
        CH -->|authenticates via| AP
    end

    SC <-->|"TCP Socket\n(ObjectStreams)"| CS

    CLIENT_SIDE -->|uses| COMMON
    SERVER_SIDE -->|uses| COMMON

    style CLIENT_SIDE fill:#1a1a2e,stroke:#e94560,stroke-width:2px,color:#eee
    style SERVER_SIDE fill:#1a1a2e,stroke:#0f3460,stroke-width:2px,color:#eee
    style COMMON fill:#16213e,stroke:#53d2dc,stroke-width:2px,color:#eee
```

---

## 2. Communication Flow Diagram

```mermaid
sequenceDiagram
    participant C as Client
    participant SC as ServerConnection
    participant AP as AuthProxyServer
    participant CS as ChatServer
    participant CH as ClientHandler
    participant CF as CommandFactory
    participant CMD as Command
    participant CR as ChatRoom

    C->>SC: connect(host, port)
    SC->>CS: TCP Socket Established
    CS->>CH: new ClientHandler(socket)
    CS->>CH: threadPool.submit(handler)

    Note over CH: Waiting for first message

    C->>SC: send(CONNECT + secret)
    SC->>CH: ObjectInputStream.readObject()
    CH->>AP: authenticate(message)
    AP-->>CH: true / false

    alt Authentication Failed
        CH-->>SC: ERROR "Invalid secret"
        CH->>CH: disconnect()
    else Authentication Success
        CH->>CF: create(message, client, server)
        CF-->>CMD: new ConnectCommand()
        CMD->>CMD: execute()
        CMD->>CS: registerClient(username)
        CMD-->>CH: sendMessage(ACK)
        CH-->>SC: ACK "Welcome!"
        SC-->>C: display welcome
    end

    Note over C,CR: Normal Operation Loop

    C->>SC: send(JOIN_ROOM "general")
    SC->>CH: readObject()
    CH->>CF: create(message)
    CF-->>CMD: new JoinRoomCommand()
    CMD->>CR: addObserver(clientHandler)
    CMD-->>CH: sendMessage(ACK)
    CMD->>CR: notifyObservers("X joined")
    CR-->>CH: onMessage(systemMsg)

    C->>SC: send(CHAT_MESSAGE "Hello!")
    SC->>CH: readObject()
    CH->>CF: create(message)
    CF-->>CMD: new SendMessageCommand()
    CMD->>CMD: stampTimestamp()
    CMD->>CR: notifyObservers(chatMsg)
    CR-->>CH: onMessage(chatMsg) [all observers]

    C->>SC: send(DISCONNECT)
    SC->>CH: readObject()
    CH->>CF: create(message)
    CF-->>CMD: new DisconnectCommand()
    CMD->>CR: removeObserver(client)
    CMD->>CS: removeClient(username)
    CMD-->>CH: sendMessage(ACK "Goodbye")
    CH->>CH: cleanup()
```

---

## 3. Server-Side Class Diagram

```mermaid
classDiagram
    direction TB

    class ServerDriver {
        +main(String[] args)$ void
        -registerShutdownHook(AuthProxyServer proxy)$ void
    }

    class Config {
        -instance$ : Config
        -properties : Map~String String~
        -Config(String filePath)
        +getInstance(String filePath)$ Config
        +getInstance()$ Config
        +reset()$ void
        +get(String key) String
        +getInt(String key, int defaultValue) int
        +has(String key) boolean
        -loadFromFile(String filePath) void
    }

    class AuthProxyServer {
        -realServer : ChatServer
        -secret : String
        +AuthProxyServer(ChatServer server, String secret)
        +start() void
        +shutdown() void
        +authenticate(Message msg) boolean
    }

    class ChatServer {
        -port : int
        -serverSocket : ServerSocket
        -clients : ConcurrentHashMap~String ClientHandler~
        -roomManager : ChatRoomManager
        -threadPool : ExecutorService
        -running : AtomicBoolean
        -authProxy : AuthProxyServer
        +ChatServer(int port)
        +setAuthProxy(AuthProxyServer proxy) void
        +start() void
        +shutdown() void
        +registerClient(String username, ClientHandler handler) void
        +removeClient(String username) void
        +isUsernameTaken(String name) boolean
        +getRoomManager() ChatRoomManager
    }

    class ClientHandler {
        -socket : Socket
        -inputStream : ObjectInputStream
        -outputStream : ObjectOutputStream
        -username : String
        -authenticated : boolean
        -running : boolean
        -server : ChatServer
        -authProxy : AuthProxyServer
        -currentRoom : String
        +ClientHandler(Socket s, ChatServer srv, AuthProxyServer auth)
        +run() void
        +sendMessage(Message msg) void
        +disconnect() void
        -cleanup() void
        +onMessage(Message msg) void
        +getObserverName() String
        +getUsername() String
        +setUsername(String name) void
        +getCurrentRoom() String
        +setCurrentRoom(String room) void
    }

    class ChatRoomManager {
        -rooms : ConcurrentHashMap~String ChatRoom~
        +ChatRoomManager()
        +createRoom(String id, String creator) ChatRoom
        +deleteRoom(String id, String requester) boolean
        +getRoom(String id) ChatRoom
        +listRooms() String
        +removeClientFromAllRooms(ChatRoomObserver observer) void
    }

    class ChatRoom {
        -roomId : String
        -creator : String
        -observers : CopyOnWriteArrayList~ChatRoomObserver~
        +ChatRoom(String roomId, String creator)
        +addObserver(ChatRoomObserver o) void
        +removeObserver(ChatRoomObserver o) void
        +notifyObservers(Message msg) void
        +notifyAndClear(String reason) void
        +hasObserver(ChatRoomObserver o) boolean
        +getObserverCount() int
        +getRoomId() String
        +getCreator() String
        +getObservers() List~ChatRoomObserver~
    }

    class ChatRoomSubject {
        <<interface>>
        +addObserver(ChatRoomObserver o)* void
        +removeObserver(ChatRoomObserver o)* void
        +notifyObservers(Message msg)* void
    }

    class ChatRoomObserver {
        <<interface>>
        +onMessage(Message msg)* void
        +getObserverName()* String
    }

    class Command {
        <<interface>>
        +execute()* void
    }

    class CommandFactory {
        -CommandFactory()
        +create(Message msg, ClientHandler client, ChatServer server)$ Command
    }

    class ConnectCommand {
        -msg : Message
        -client : ClientHandler
        -server : ChatServer
        +ConnectCommand(Message, ClientHandler, ChatServer)
        +execute() void
    }

    class DisconnectCommand {
        -client : ClientHandler
        -server : ChatServer
        +DisconnectCommand(ClientHandler, ChatServer)
        +execute() void
    }

    class JoinRoomCommand {
        -msg : Message
        -client : ClientHandler
        -server : ChatServer
        +JoinRoomCommand(Message, ClientHandler, ChatServer)
        +execute() void
    }

    class LeaveRoomCommand {
        -msg : Message
        -client : ClientHandler
        -server : ChatServer
        +LeaveRoomCommand(Message, ClientHandler, ChatServer)
        +execute() void
    }

    class CreateRoomCommand {
        -msg : Message
        -client : ClientHandler
        -server : ChatServer
        +CreateRoomCommand(Message, ClientHandler, ChatServer)
        +execute() void
    }

    class DeleteRoomCommand {
        -msg : Message
        -client : ClientHandler
        -server : ChatServer
        +DeleteRoomCommand(Message, ClientHandler, ChatServer)
        +execute() void
    }

    class ListRoomsCommand {
        -client : ClientHandler
        -server : ChatServer
        +ListRoomsCommand(ClientHandler, ChatServer)
        +execute() void
    }

    class SendMessageCommand {
        -msg : Message
        -client : ClientHandler
        -server : ChatServer
        +SendMessageCommand(Message, ClientHandler, ChatServer)
        +execute() void
    }

    class ErrorCommand {
        -client : ClientHandler
        -errorMessage : String
        +ErrorCommand(ClientHandler, String)
        +execute() void
    }

    class Message {
        -type : MessageType
        -secret : String
        -sender : String
        -roomId : String
        -content : String
        -timestamp : long
        +Message()
        +Message(MessageType, String, String)
        +Message(MessageType, String, String, String, String)
        +stampTimestamp() void
        +getType() MessageType
        +setType(MessageType) void
        +getSecret() String
        +getSender() String
        +getRoomId() String
        +getContent() String
        +getTimestamp() long
    }

    class MessageType {
        <<enumeration>>
        CONNECT
        DISCONNECT
        LIST_ROOMS
        CREATE_ROOM
        DELETE_ROOM
        JOIN_ROOM
        LEAVE_ROOM
        CHAT_MESSAGE
        ACK
        ERROR
        SYSTEM_MESSAGE
        ROOM_LIST
    }

    %% ===== RELATIONSHIPS =====

    %% Dependency: ServerDriver creates AuthProxyServer
    ServerDriver ..> AuthProxyServer : creates
    ServerDriver ..> Config : uses

    %% Composition: AuthProxyServer wraps ChatServer (Proxy Pattern)
    AuthProxyServer *-- ChatServer : wraps

    %% Composition: ChatServer owns ChatRoomManager
    ChatServer *-- ChatRoomManager : owns

    %% Aggregation: ChatServer manages multiple ClientHandlers
    ChatServer o-- "0..*" ClientHandler : manages

    %% Composition: ChatRoomManager owns ChatRooms
    ChatRoomManager *-- "0..*" ChatRoom : owns

    %% Realization: ChatRoom implements ChatRoomSubject
    ChatRoomSubject <|.. ChatRoom : implements

    %% Realization: ClientHandler implements ChatRoomObserver
    ChatRoomObserver <|.. ClientHandler : implements

    %% Realization: ClientHandler implements Runnable
    Runnable <|.. ClientHandler : implements

    %% Association: ChatRoom observes many ChatRoomObservers
    ChatRoom --> "0..*" ChatRoomObserver : notifies

    %% Dependency: ClientHandler uses AuthProxyServer
    ClientHandler ..> AuthProxyServer : authenticates via

    %% Dependency: ClientHandler uses CommandFactory
    ClientHandler ..> CommandFactory : delegates to

    %% Dependency: CommandFactory creates Command objects
    CommandFactory ..> Command : creates

    %% Realization: All concrete commands implement Command
    Command <|.. ConnectCommand : implements
    Command <|.. DisconnectCommand : implements
    Command <|.. JoinRoomCommand : implements
    Command <|.. LeaveRoomCommand : implements
    Command <|.. CreateRoomCommand : implements
    Command <|.. DeleteRoomCommand : implements
    Command <|.. ListRoomsCommand : implements
    Command <|.. SendMessageCommand : implements
    Command <|.. ErrorCommand : implements

    %% Dependency: Commands use ChatServer
    ConnectCommand ..> ChatServer : uses
    DisconnectCommand ..> ChatServer : uses
    JoinRoomCommand ..> ChatServer : uses
    LeaveRoomCommand ..> ChatServer : uses
    CreateRoomCommand ..> ChatServer : uses
    DeleteRoomCommand ..> ChatServer : uses
    ListRoomsCommand ..> ChatServer : uses
    SendMessageCommand ..> ChatServer : uses

    %% Dependency: Commands use ClientHandler
    ConnectCommand ..> ClientHandler : uses
    SendMessageCommand ..> ChatRoom : broadcasts via

    %% Association: Message contains MessageType
    Message --> MessageType : has

    %% Config is Singleton
    Config --> Config : instance
```

---

## 4. Client-Side Class Diagram

```mermaid
classDiagram
    direction TB

    class ClientDriver {
        +main(String[] args)$ void
    }

    class Config {
        -instance$ : Config
        -properties : Map~String String~
        -Config(String filePath)
        +getInstance(String filePath)$ Config
        +getInstance()$ Config
        +reset()$ void
        +get(String key) String
        +getInt(String key, int defaultValue) int
        +has(String key) boolean
        -loadFromFile(String filePath) void
    }

    class ChatClient {
        -username : String
        -host : String
        -port : int
        -secret : String
        -connection : ServerConnection
        -listener : MessageListener
        -menuHandler : MenuHandler
        -running : AtomicBoolean
        -inChatMode : AtomicBoolean
        -currentRoom : String
        +ChatClient(String user, String host, int port, String secret)
        +start() void
        +handleIncoming(Message msg) void
        +onConnectionLost() void
        +listRooms() void
        +createRoom(String roomId) void
        +joinRoom(String roomId) void
        +leaveRoom() void
        +deleteRoom(String roomId) void
        +sendChat(String text) void
        +quit() void
        -sendToServer(Message msg) void
        -cleanup() void
        +isRunning() boolean
        +isInChatMode() boolean
        +getUsername() String
    }

    class ServerConnection {
        -socket : Socket
        -outputStream : ObjectOutputStream
        -inputStream : ObjectInputStream
        -connected : AtomicBoolean
        +ServerConnection()
        +connect(String host, int port) void
        +send(Message msg) void
        +receive() Message
        +disconnect() void
        +isConnected() boolean
    }

    class MessageListener {
        -connection : ServerConnection
        -chatClient : ChatClient
        -running : boolean
        +MessageListener(ServerConnection conn, ChatClient client)
        +run() void
        +stop() void
    }

    class MenuHandler {
        -scanner : Scanner
        -chatClient : ChatClient
        +MenuHandler(ChatClient client)
        +run() void
        -showMainMenu() void
        -handleMainMenuInput() void
        -handleChatMode() void
        +displayMessage(String text) void
        +showChatModeHeader(String roomId) void
        -readLine() String
        -pause() void
        -padRight(String s, int n) String
    }

    class Message {
        -type : MessageType
        -secret : String
        -sender : String
        -roomId : String
        -content : String
        -timestamp : long
        +Message()
        +Message(MessageType, String, String)
        +Message(MessageType, String, String, String, String)
        +stampTimestamp() void
        +getType() MessageType
        +getSender() String
        +getRoomId() String
        +getContent() String
        +getTimestamp() long
    }

    class MessageType {
        <<enumeration>>
        CONNECT
        DISCONNECT
        LIST_ROOMS
        CREATE_ROOM
        DELETE_ROOM
        JOIN_ROOM
        LEAVE_ROOM
        CHAT_MESSAGE
        ACK
        ERROR
        SYSTEM_MESSAGE
        ROOM_LIST
    }

    class Runnable {
        <<interface>>
        +run()* void
    }

    %% ===== RELATIONSHIPS =====

    %% Dependency: ClientDriver creates ChatClient
    ClientDriver ..> ChatClient : creates
    ClientDriver ..> Config : uses

    %% Composition: ChatClient owns ServerConnection
    ChatClient *-- ServerConnection : owns

    %% Composition: ChatClient owns MessageListener
    ChatClient *-- MessageListener : owns

    %% Composition: ChatClient owns MenuHandler
    ChatClient *-- MenuHandler : owns

    %% Association: MessageListener reads from ServerConnection
    MessageListener --> ServerConnection : reads from

    %% Association: MessageListener delegates to ChatClient
    MessageListener --> ChatClient : delegates incoming

    %% Association: MenuHandler invokes ChatClient actions
    MenuHandler --> ChatClient : invokes actions

    %% Realization: MessageListener implements Runnable
    Runnable <|.. MessageListener : implements

    %% Dependency: ChatClient uses Message
    ChatClient ..> Message : creates and sends

    %% Dependency: ServerConnection transports Message
    ServerConnection ..> Message : transports

    %% Association: Message contains MessageType
    Message --> MessageType : has

    %% Config Singleton self-reference
    Config --> Config : instance
```

---

## 5. Design Patterns Highlight Diagram

```mermaid
graph LR
    subgraph SINGLETON["🔷 Singleton Pattern"]
        direction TB
        CS_INST["Config\n(private static instance)"]
        CS_GET["getInstance()"]
        CS_INST --- CS_GET
    end

    subgraph PROXY["🔷 Proxy Pattern"]
        direction TB
        CLIENT_REQ["Client Request"] --> PROXY_SRV["AuthProxyServer\nauthenticate()"]
        PROXY_SRV -->|"secret valid"| REAL_SRV["ChatServer\n(Real Subject)"]
        PROXY_SRV -->|"secret invalid"| REJECT["Reject Connection"]
    end

    subgraph OBSERVER["🔷 Observer Pattern"]
        direction TB
        SUBJECT["ChatRoom\n(Subject)"] -->|notifyObservers| OBS1["ClientHandler A\n(Observer)"]
        SUBJECT -->|notifyObservers| OBS2["ClientHandler B\n(Observer)"]
        SUBJECT -->|notifyObservers| OBS3["ClientHandler C\n(Observer)"]
    end

    subgraph COMMAND["🔷 Command Pattern"]
        direction TB
        INVOKER["ClientHandler\n(Invoker)"] --> FACTORY["CommandFactory\n(Factory)"]
        FACTORY --> CMD1["ConnectCommand"]
        FACTORY --> CMD2["JoinRoomCommand"]
        FACTORY --> CMD3["SendMessageCommand"]
        FACTORY --> CMD4["DisconnectCommand"]
        FACTORY --> CMD5["...others"]
        CMD1 --> RECEIVER["ChatServer / ChatRoom\n(Receiver)"]
        CMD2 --> RECEIVER
        CMD3 --> RECEIVER
        CMD4 --> RECEIVER
    end

    subgraph FACTORY_PAT["🔷 Factory Method Pattern"]
        direction TB
        MSG_IN["Message.type"] --> CF["CommandFactory.create()"]
        CF --> |"CONNECT"| CC["new ConnectCommand()"]
        CF --> |"JOIN_ROOM"| JC["new JoinRoomCommand()"]
        CF --> |"CHAT_MESSAGE"| SC2["new SendMessageCommand()"]
        CF --> |"..."| OC["new ...Command()"]
    end

    style SINGLETON fill:#0d1b2a,stroke:#e0c3fc,stroke-width:2px,color:#eee
    style PROXY fill:#0d1b2a,stroke:#ffd6a5,stroke-width:2px,color:#eee
    style OBSERVER fill:#0d1b2a,stroke:#caffbf,stroke-width:2px,color:#eee
    style COMMAND fill:#0d1b2a,stroke:#9bf6ff,stroke-width:2px,color:#eee
    style FACTORY_PAT fill:#0d1b2a,stroke:#ffc6ff,stroke-width:2px,color:#eee
```

---

## 6. Thread Model Diagram

```mermaid
graph TB
    subgraph SERVER_THREADS["🗄️ Server Threads"]
        MAIN_T["Main Thread\n(ServerDriver)"]
        ACCEPT_T["Accept Loop\n(ChatServer.start)"]
        TP["Thread Pool\n(FixedThreadPool 50)"]
        CH1["ClientHandler Thread 1\n(Alice)"]
        CH2["ClientHandler Thread 2\n(Bob)"]
        CH3["ClientHandler Thread 3\n(Charlie)"]
        SHUTDOWN_T["Shutdown Hook Thread\n(Ctrl+C Handler)"]

        MAIN_T -->|"starts"| ACCEPT_T
        ACCEPT_T -->|"submit()"| TP
        TP -->|"executes"| CH1
        TP -->|"executes"| CH2
        TP -->|"executes"| CH3
        MAIN_T -.->|"registers"| SHUTDOWN_T
        SHUTDOWN_T -.->|"calls shutdown()"| ACCEPT_T
    end

    subgraph CLIENT_THREADS["🖥️ Client Threads (per client)"]
        C_MAIN["Main Thread\n(ClientDriver)"]
        C_MENU["Menu Thread\n(MenuHandler.run)"]
        C_LISTEN["Listener Daemon Thread\n(MessageListener.run)"]

        C_MAIN -->|"starts"| C_MENU
        C_MAIN -->|"starts daemon"| C_LISTEN
        C_MENU -->|"user input → send"| SOCKET["Socket"]
        C_LISTEN -->|"receive → display"| SOCKET
    end

    CH1 <-->|"TCP"| SOCKET

    style SERVER_THREADS fill:#1a1a2e,stroke:#0f3460,stroke-width:2px,color:#eee
    style CLIENT_THREADS fill:#1a1a2e,stroke:#e94560,stroke-width:2px,color:#eee
```

---

