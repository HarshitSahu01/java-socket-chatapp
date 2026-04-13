package common;

import java.io.Serializable;

public enum MessageType implements Serializable {
    CONNECT, DISCONNECT,
    LIST_ROOMS, CREATE_ROOM, DELETE_ROOM,
    JOIN_ROOM, LEAVE_ROOM,
    CHAT_MESSAGE,
    ACK, ERROR, SYSTEM_MESSAGE, ROOM_LIST
}