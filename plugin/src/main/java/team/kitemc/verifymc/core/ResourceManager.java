package team.kitemc.verifymc.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.function.BiFunction;

/**
 * Manages plugin resources: config files, language files, email templates,
 * and static web assets (front-end themes).
 * <p>
 * Extracted from the original 504-line ResourceManager into a cleaner design
 * that delegates i18n to {@link I18nManager}.
 * <p>
 * Implements BiFunction&lt;String, String, String&gt; for use as an i18n message resolver.
 */
public class ResourceManager implements BiFunction<String, String, String> {
    private final JavaPlugin plugin;
    private I18nManager i18nManager;

    public ResourceManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Set the I18nManager for message resolution.
     */
    public void setI18nManager(I18nManager i18nManager) {
        this.i18nManager = i18nManager;
    }

    /**
     * BiFunction apply method for i18n message resolution.
     * @param key The message key
     * @param language The language code
     * @return The localized message
     */
    @Override
    public String apply(String key, String language) {
        if (i18nManager != null) {
            return i18nManager.getMessage(key, language);
        }
        return key;
    }

    /**
     * Initialize all resources: extract default config, i18n, email templates, static.
     */
    public void init() {
        plugin.saveDefaultConfig();
        saveResourceIfNotExists("config_help_zh.yml");
        saveResourceIfNotExists("config_help_en.yml");
        extractDirectoryFromJar("i18n");
        extractDirectoryFromJar("email");
        extractStaticAssets();
    }

    /**
     * Save a resource file from the JAR to the data folder if it doesn't already exist.
     */
    private void saveResourceIfNotExists(String resourceName) {
        File outFile = new File(plugin.getDataFolder(), resourceName);
        if (!outFile.exists()) {
            plugin.saveResource(resourceName, false);
        }
    }

    /**
     * Extract a directory from the plugin JAR to the data folder,
     * only creating files that don't already exist.
     */
    public void extractDirectoryFromJar(String dirName) {
        File targetDir = new File(plugin.getDataFolder(), dirName);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarFile.isFile()) return;

            try (JarFile jar = new JarFile(jarFile)) {
                jar.stream()
                    .filter(e -> e.getName().startsWith(dirName + "/") && !e.isDirectory())
                    .forEach(entry -> {
                        File outFile = new File(plugin.getDataFolder(), entry.getName());
                        if (!outFile.exists()) {
                            outFile.getParentFile().mkdirs();
                            try (InputStream is = jar.getInputStream(entry);
                                 OutputStream os = new FileOutputStream(outFile)) {
                                byte[] buffer = new byte[4096];
                                int len;
                                while ((len = is.read(buffer)) != -1) {
                                    os.write(buffer, 0, len);
                                }
                            } catch (IOException e) {
                                plugin.getLogger().warning("Failed to extract " + entry.getName() + ": " + e.getMessage());
                            }
                        }
                    });
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to extract directory '" + dirName + "': " + e.getMessage());
        }
    }

    /**
     * Extract static web assets (themes).
     */
    private void extractStaticAssets() {
        extractDirectoryFromJar("static");
    }

    /**
     * Get the path to a static resource file.
     */
    public File getStaticDir() {
        return new File(plugin.getDataFolder(), "static");
    }

    /**
     * Read a file as a string (UTF-8).
     */
    public String readFileAsString(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read file: " + file.getAbsolutePath());
            return "";
        }
    }

    /**
     * Read the email template for the given type and language.
     *
     * @param type     e.g. "verify_code" or "review_approved"
     * @param language e.g. "en" or "zh"
     */
    public String getEmailTemplate(String type, String language) {
        File emailDir = new File(plugin.getDataFolder(), "email");

        // Try language-specific template first
        File langFile = new File(emailDir, type + "_" + language + ".html");
        if (langFile.exists()) {
            return readFileAsString(langFile);
        }

        // Fallback to English
        File enFile = new File(emailDir, type + "_en.html");
        if (enFile.exists()) {
            return readFileAsString(enFile);
        }

        return "<html><body><p>Template not found: " + type + "</p></body></html>";
    }

    /**
     * Create a backup of the config.yml before reloading.
     */
    public void backupConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            File backupFile = new File(plugin.getDataFolder(), "config.yml.bak");
            try {
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to backup config: " + e.getMessage());
            }
        }
    }
}
