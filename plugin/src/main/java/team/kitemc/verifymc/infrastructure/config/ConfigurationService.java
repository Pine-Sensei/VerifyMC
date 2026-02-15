package team.kitemc.verifymc.infrastructure.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ConfigurationService {

    private final JavaPlugin plugin;
    private volatile FileConfiguration config;
    private final Map<String, Set<Consumer<Object>>> changeListeners = new ConcurrentHashMap<>();
    private final Map<String, Object> cachedValues = new ConcurrentHashMap<>();
    private final Logger logger;

    public ConfigurationService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.logger = plugin.getLogger();
        plugin.saveDefaultConfig();
    }

    public boolean isDebug() {
        return getBoolean("debug", false);
    }

    public String getLanguage() {
        return getString("language", "en");
    }

    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    public Logger getLogger() {
        return logger;
    }

    public String getWebServerPrefix() {
        return getString("web_server_prefix", "[VerifyMC]");
    }

    public void reload() {
        Map<String, Object> oldValues = new HashMap<>(cachedValues);
        
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        cachedValues.clear();
        
        notifyChanges(oldValues);
    }

    public String getString(String path) {
        return getString(path, "");
    }

    public String getString(String path, String defaultValue) {
        String value = cachedValues.computeIfAbsent(path, p -> 
            config.getString(p, defaultValue)).toString();
        return value;
    }

    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int defaultValue) {
        Object cached = cachedValues.get(path);
        if (cached instanceof Integer) {
            return (Integer) cached;
        }
        
        int value = config.getInt(path, defaultValue);
        cachedValues.put(path, value);
        return value;
    }

    public long getLong(String path) {
        return getLong(path, 0L);
    }

    public long getLong(String path, long defaultValue) {
        Object cached = cachedValues.get(path);
        if (cached instanceof Long) {
            return (Long) cached;
        }
        
        long value = config.getLong(path, defaultValue);
        cachedValues.put(path, value);
        return value;
    }

    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }

    public double getDouble(String path, double defaultValue) {
        Object cached = cachedValues.get(path);
        if (cached instanceof Double) {
            return (Double) cached;
        }
        
        double value = config.getDouble(path, defaultValue);
        cachedValues.put(path, value);
        return value;
    }

    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        Object cached = cachedValues.get(path);
        if (cached instanceof Boolean) {
            return (Boolean) cached;
        }
        
        boolean value = config.getBoolean(path, defaultValue);
        cachedValues.put(path, value);
        return value;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path) {
        Object cached = cachedValues.get(path);
        if (cached instanceof List) {
            return new ArrayList<>((List<String>) cached);
        }
        
        List<String> value = config.getStringList(path);
        cachedValues.put(path, new ArrayList<>(value));
        return new ArrayList<>(value);
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getIntegerList(String path) {
        Object cached = cachedValues.get(path);
        if (cached instanceof List) {
            return new ArrayList<>((List<Integer>) cached);
        }
        
        List<Integer> value = config.getIntegerList(path);
        cachedValues.put(path, new ArrayList<>(value));
        return new ArrayList<>(value);
    }

    public List<?> getList(String path) {
        return getList(path, Collections.emptyList());
    }

    public List<?> getList(String path, List<?> defaultValue) {
        Object cached = cachedValues.get(path);
        if (cached instanceof List) {
            return new ArrayList<>((List<?>) cached);
        }
        
        List<?> value = config.getList(path, defaultValue);
        cachedValues.put(path, new ArrayList<>(value));
        return new ArrayList<>(value);
    }

    public Set<String> getKeys(String path) {
        if (path == null || path.isEmpty()) {
            return config.getKeys(false);
        }
        
        Object section = config.get(path);
        if (section instanceof org.bukkit.configuration.ConfigurationSection) {
            return ((org.bukkit.configuration.ConfigurationSection) section).getKeys(false);
        }
        return Collections.emptySet();
    }

    public boolean contains(String path) {
        return config.contains(path);
    }

    public Object get(String path) {
        return get(path, null);
    }

    public Object get(String path, Object defaultValue) {
        return config.get(path, defaultValue);
    }

    public ConfigurationService getChild(String path) {
        return new ChildConfigurationService(this, path);
    }

    public void registerChangeListener(String path, Consumer<Object> listener) {
        changeListeners.computeIfAbsent(path, k -> ConcurrentHashMap.newKeySet()).add(listener);
    }

    public void unregisterChangeListener(String path, Consumer<Object> listener) {
        Set<Consumer<Object>> listeners = changeListeners.get(path);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public void unregisterAllListeners() {
        changeListeners.clear();
    }

    private void notifyChanges(Map<String, Object> oldValues) {
        for (Map.Entry<String, Set<Consumer<Object>>> entry : changeListeners.entrySet()) {
            String path = entry.getKey();
            Object oldValue = oldValues.get(path);
            Object newValue = get(path);
            
            if (!Objects.equals(oldValue, newValue)) {
                for (Consumer<Object> listener : entry.getValue()) {
                    try {
                        listener.accept(newValue);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error notifying config change listener for " + path + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    public void set(String path, Object value) {
        config.set(path, value);
        cachedValues.remove(path);
    }

    public void save() {
        plugin.saveConfig();
    }

    public FileConfiguration getRawConfig() {
        return config;
    }

    private static class ChildConfigurationService extends ConfigurationService {
        private final ConfigurationService parent;
        private final String prefix;

        ChildConfigurationService(ConfigurationService parent, String prefix) {
            super(parent.plugin);
            this.parent = parent;
            this.prefix = prefix.endsWith(".") ? prefix : prefix + ".";
        }

        private String fullPath(String path) {
            return prefix + path;
        }

        @Override
        public String getString(String path, String defaultValue) {
            return parent.getString(fullPath(path), defaultValue);
        }

        @Override
        public int getInt(String path, int defaultValue) {
            return parent.getInt(fullPath(path), defaultValue);
        }

        @Override
        public long getLong(String path, long defaultValue) {
            return parent.getLong(fullPath(path), defaultValue);
        }

        @Override
        public double getDouble(String path, double defaultValue) {
            return parent.getDouble(fullPath(path), defaultValue);
        }

        @Override
        public boolean getBoolean(String path, boolean defaultValue) {
            return parent.getBoolean(fullPath(path), defaultValue);
        }

        @Override
        public List<String> getStringList(String path) {
            return parent.getStringList(fullPath(path));
        }

        @Override
        public List<Integer> getIntegerList(String path) {
            return parent.getIntegerList(fullPath(path));
        }

        @Override
        public List<?> getList(String path, List<?> defaultValue) {
            return parent.getList(fullPath(path), defaultValue);
        }

        @Override
        public Set<String> getKeys(String path) {
            return parent.getKeys(fullPath(path));
        }

        @Override
        public boolean contains(String path) {
            return parent.contains(fullPath(path));
        }

        @Override
        public Object get(String path, Object defaultValue) {
            return parent.get(fullPath(path), defaultValue);
        }

        @Override
        public void set(String path, Object value) {
            parent.set(fullPath(path), value);
        }

        @Override
        public void registerChangeListener(String path, Consumer<Object> listener) {
            parent.registerChangeListener(fullPath(path), listener);
        }

        @Override
        public void unregisterChangeListener(String path, Consumer<Object> listener) {
            parent.unregisterChangeListener(fullPath(path), listener);
        }
    }
}
