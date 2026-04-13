package common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Config {

    private static volatile Config instance = null;
    private final Map<String, String> properties;

    private Config(String filePath) {
        properties = new HashMap<>();
        loadFromFile(filePath);
    }

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

    public static Config getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Config not initialized. Call getInstance(filePath) first.");
        }
        return instance;
    }

    public String get(String key) {
        return properties.getOrDefault(key, "");
    }

    public int getInt(String key, int defaultValue) {
        String val = properties.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean has(String key) {
        return properties.containsKey(key);
    }

    private void loadFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    properties.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
            System.out.println("[Config] Loaded " + properties.size() + " properties from " + filePath);
        } catch (IOException e) {
            System.err.println("[Config] ERROR: Cannot read file: " + filePath);
        }
    }
}