package common;

import java.io.Serializable;

/**
 * Enum defining all message types in the protocol.
 * Shared between client and server.
 */
public enum MessageType implements Serializable {
    // Client to Server requests
    CONNECT,
    DISCONNECT,
    LIST_ROOMS,
    CREATE_ROOM,
    DELETE_ROOM,
    JOIN_ROOM,
    LEAVE_ROOM,
    CHAT_MESSAGE,

    // Server to Client responses
    ACK,
    ERROR,
    SYSTEM_MESSAGE,
    ROOM_LIST
}