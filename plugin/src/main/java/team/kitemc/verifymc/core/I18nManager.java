package team.kitemc.verifymc.core;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Unified i18n management. Loads property-based resource bundles from the
 * plugin data folder or from the JAR, with caching.
 */
public class I18nManager {
    private final JavaPlugin plugin;
    private final HashMap<String, ResourceBundle> languageCache = new HashMap<>();
    private ResourceBundle defaultBundle;

    public I18nManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize with a default language bundle.
     */
    public void init(String defaultLanguage) {
        this.defaultBundle = loadBundle(defaultLanguage);
    }

    /**
     * Get a localized message for the given key and language.
     */
    public String getMessage(String key, String language) {
        ResourceBundle bundle = getBundle(language);
        if (bundle != null && bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return key;
    }

    /**
     * Get or load the language bundle, with caching.
     */
    public ResourceBundle getBundle(String language) {
        if (languageCache.containsKey(language)) {
            return languageCache.get(language);
        }

        ResourceBundle bundle = loadBundle(language);
        if (bundle != null) {
            languageCache.put(language, bundle);
            return bundle;
        }

        // Fallback to default
        return defaultBundle;
    }

    /**
     * Load a resource bundle for the given language.
     * First tries the i18n directory in the plugin data folder,
     * then falls back to the JAR classpath.
     */
    private ResourceBundle loadBundle(String language) {
        // Try loading from data folder
        File i18nDir = new File(plugin.getDataFolder(), "i18n");
        File propFile = new File(i18nDir, "messages_" + language + ".properties");

        if (propFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(propFile), StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(reader);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load i18n file: " + propFile.getName() + " - " + e.getMessage());
            }
        }

        // Try loading from JAR
        try {
            return ResourceBundle.getBundle("i18n.messages", new Locale(language));
        } catch (Exception e) {
            // Fallback
            return null;
        }
    }

    /**
     * Clear the language cache (e.g., after config reload).
     */
    public void clearCache() {
        languageCache.clear();
    }

    /**
     * Get the default resource bundle.
     * @return The default ResourceBundle, or null if not initialized.
     */
    public ResourceBundle getResourceBundle() {
        return defaultBundle;
    }
}
