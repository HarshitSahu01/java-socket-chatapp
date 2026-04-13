package server.observer;

import common.Message;
import java.io.IOException;

/**
 * Observer Pattern — Observer Interface.
 *
 * Any class that wants to receive messages from a ChatRoom
 * must implement this interface.
 */
public interface ChatRoomObserver {

    /**
     * Called when a new message is broadcast in the room.
     * @param msg The message to deliver.
     * @throws IOException if delivery fails (client disconnected).
     */
    void onMessage(Message msg) throws IOException;

    /**
     * Get the identifier of this observer (username).
     */
    String getObserverName();
}