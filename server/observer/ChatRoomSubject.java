package server.observer;

import common.Message;

/**
 * Observer Pattern — Subject Interface.
 *
 * Defines the contract for observable chat rooms.
 */
public interface ChatRoomSubject {

    /**
     * Register an observer to this subject.
     */
    void addObserver(ChatRoomObserver observer);

    /**
     * Remove an observer from this subject.
     */
    void removeObserver(ChatRoomObserver observer);

    /**
     * Notify all registered observers with a message.
     */
    void notifyObservers(Message msg);
}