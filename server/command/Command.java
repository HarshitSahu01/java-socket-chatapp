package server.command;

/**
 * Command Pattern — Command Interface.
 *
 * All concrete commands implement this single method.
 */
public interface Command {

    /**
     * Execute the command.
     */
    void execute();
}