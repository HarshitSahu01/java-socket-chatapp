package common;

/**
 * Shared constants for the protocol.
 */
public final class ProtocolConstants {

    private ProtocolConstants() {
        // Prevent instantiation
    }

    public static final int DEFAULT_PORT = 9000;
    public static final int SOCKET_TIMEOUT_MS = 0; // 0 = infinite blocking read
    public static final int MAX_MESSAGE_SIZE = 65536;
    public static final String DEFAULT_HOST = "localhost";
}