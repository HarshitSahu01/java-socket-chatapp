package common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton Pattern (standard implementation).
 *
 * Loads a .env file once and provides key-value access.
 * Thread-safe with double-checked locking.
 */
public class Config {

    // Volatile instance for double-checked locking
    private static volatile Config instance = null;

    // Immutable map of config values
    private final Map<String, String> properties;

    // Private constructor — loads file
    private Config(String filePath) {
        properties = new HashMap<>();
        loadFromFile(filePath);
    }

    /**
     * Get the singleton instance. First call must provide a valid filePath.
     * Subsequent calls can pass null (instance already created).
     */
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

    /**
     * Convenience overload — use after initial creation.
     */
    public static Config getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "Config not initialized. Call getInstance(filePath) first.");
        }
        return instance;
    }

    /**
     * Reset the singleton (useful for testing or reloading).
     */
    public static synchronized void reset() {
        instance = null;
    }

    /**
     * Get value by key. Returns empty string if not found.
     */
    public String get(String key) {
        return properties.getOrDefault(key, "");
    }

    /**
     * Get value as integer. Returns defaultValue if not found or parse error.
     */
    public int getInt(String key, int defaultValue) {
        String val = properties.get(key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Check if a key exists.
     */
    public boolean has(String key) {
        return properties.containsKey(key);
    }

    /**
     * Load key=value pairs from a .env file.
     * Ignores blank lines and lines starting with #.
     */
    private void loadFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    properties.put(key, value);
                }
            }
            System.out.println("[Config] Loaded " + properties.size()
                               + " properties from " + filePath);
        } catch (IOException e) {
            System.err.println("[Config] ERROR: Cannot read file: "
                               + filePath);
            System.err.println("[Config] " + e.getMessage());
        }
    }
}