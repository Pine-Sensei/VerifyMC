package team.kitemc.verifymc;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.apache.commons.io.FileUtils;

public class ResourceManager {
    private final JavaPlugin plugin;
    private final boolean debug;

    public ResourceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
    }

    private void debugLog(String msg) {
        if (debug) plugin.getLogger().info("[DEBUG] ResourceManager: " + msg);
    }

    private String getMessage(String key, String lang) {
        try {
            ResourceBundle messages = ResourceBundle.getBundle("i18n.messages", new Locale(lang));
            return messages.containsKey(key) ? messages.getString(key) : key;
        } catch (Exception e) {
            // During reload, the classloader is closed, so we return English hardcoded messages
            if ("error.resource_init_failed".equals(key)) return "Failed to initialize resources";
            if ("error.resource_update_check_failed".equals(key)) return "Failed to check resource updates";
            return key;
        }
    }

    private String getConfigLanguage() {
        return plugin.getConfig().getString("language", "en");
    }

    private void backupPluginDataFolder() {
        boolean backupOnUpdate = plugin.getConfig().getBoolean("backup_on_update", true);
        if (!backupOnUpdate) {
            debugLog("Backup on update is disabled.");
            return;
        }

        File sourceDir = plugin.getDataFolder();
        String timestamp = String.valueOf(System.currentTimeMillis());
        File backupDir = new File(sourceDir, "backup/" + timestamp);

        if (!backupDir.mkdirs()) {
            plugin.getLogger().warning("[VerifyMC] Failed to create backup directory: " + backupDir.getAbsolutePath());
            return;
        }

        // Exclude the 'backup' directory itself to prevent recursion.
        FileFilter filter = file -> !file.getAbsolutePath().startsWith(new File(sourceDir, "backup").getAbsolutePath());

        try {
            FileUtils.copyDirectory(sourceDir, backupDir, filter);
            plugin.getLogger().info("[VerifyMC] Files auto-backed up to: " + backupDir.getAbsolutePath());
        } catch (IOException e) {
            plugin.getLogger().warning("[VerifyMC] Full backup failed: " + e.getMessage());
            debugLog("Full backup failed: " + e.toString());
        }
    }

    /**
     * Initialize all resource directories and files
     */
    public void initializeResources() {
        debugLog("Initializing resources...");
        
        // Backup first
        backupPluginDataFolder();
        
        try {
            // Create base directories
            createDirectories();

            // Save help files
            plugin.saveResource("config_help_en.yml", false);
            plugin.saveResource("config_help_zh.yml", false);
            
            // Update configuration file
            upgradeConfigFile();
            
            // Update i18n files
            updateI18nFiles();
            
            // Update email templates
            patchEmailTemplates();
            
            // Update static files
            updateStaticThemes();
            
            debugLog("Resource initialization completed");
        } catch (Exception e) {
            plugin.getLogger().warning(getMessage("error.resource_init_failed", getConfigLanguage()) + ": " + e.getMessage());
            debugLog("Resource initialization failed: " + e.getMessage());
        }
    }

    /**
     * Create necessary directory structure
     */
    private void createDirectories() {
        debugLog("Creating directories...");
        
        String[] dirs = {
            "i18n",
            "email", 
            "static/default",
            "static/glassx",
            "data",
            "backup" // Added backup directory
        };
        
        for (String dir : dirs) {
            File directory = new File(plugin.getDataFolder(), dir);
            if (!directory.exists()) {
                directory.mkdirs();
                debugLog("Created directory: " + directory.getAbsolutePath());
            } else {
                debugLog("Directory already exists: " + directory.getAbsolutePath());
            }
        }
    }

    /**
     * Update configuration file
     */
    private void upgradeConfigFile() {
        boolean autoUpdateConfig = plugin.getConfig().getBoolean("auto_update_config", false);
        if (!autoUpdateConfig) return;
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(plugin.getResource("config.yml"), StandardCharsets.UTF_8));
            YamlConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
            boolean changed = false;
            for (String key : defaultConfig.getKeys(true)) {
                if (!currentConfig.contains(key)) {
                    currentConfig.set(key, defaultConfig.get(key));
                    changed = true;
                }
            }
            if (changed) {
                currentConfig.save(configFile);
                plugin.getLogger().info("[VerifyMC] Config file auto-upgraded");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[VerifyMC] Config upgrade failed: " + e.getMessage());
        }
    }

    /**
     * Update i18n files
     */
    private void updateI18nFiles() {
        boolean autoUpdateI18n = plugin.getConfig().getBoolean("auto_update_i18n", true);
        if (!autoUpdateI18n) return;
        debugLog("Updating i18n files...");
        File i18nDir = new File(plugin.getDataFolder(), "i18n");
        // Scan all messages_*.properties files
        File[] existingFiles = i18nDir.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".properties"));
        if (existingFiles != null) {
            for (File propFile : existingFiles) {
                String fileName = propFile.getName();
                String lang = fileName.substring("messages_".length(), fileName.length() - ".properties".length());
                debugLog("Found existing i18n file: " + fileName + " (language: " + lang + ")");
                updateI18nFileIfNeeded(propFile, lang);
            }
        }
        // Ensure built-in zh and en files exist
        String[] builtinLanguages = {"zh", "en"};
        for (String lang : builtinLanguages) {
            File propFile = new File(i18nDir, "messages_" + lang + ".properties");
            if (!propFile.exists()) {
                try (InputStream in = plugin.getResource("i18n/messages_" + lang + ".properties");
                     OutputStream out = new FileOutputStream(propFile)) {
                    if (in != null) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                        debugLog("Created builtin i18n file: " + propFile.getName());
                    } else {
                        debugLog("Default i18n resource not found in JAR: i18n/messages_" + lang + ".properties");
                    }
                } catch (Exception e) {
                    debugLog("Failed to create builtin i18n file " + lang + ": " + e.getMessage());
                }
            }
        }
        // Backup and update built-in files
        plugin.saveResource("i18n/messages_zh.properties", true);
        plugin.saveResource("i18n/messages_en.properties", true);
        plugin.getLogger().info("[VerifyMC] Language files auto-updated");
    }

    /**
     * Check and update i18n file if needed
     * @param propFile Properties file to check
     * @param lang Language code
     */
    private void updateI18nFileIfNeeded(File propFile, String lang) {
        try {
            debugLog("Checking i18n file: " + propFile.getName() + " (language: " + lang + ")");
            String currentContent = new String(Files.readAllBytes(propFile.toPath()), StandardCharsets.UTF_8);
            
            // Check if it contains new version key messages
            String[] requiredKeys = {
                "admin.unban",
                "admin.unbanSuccess",
                "admin.unbanFailed",
                "admin.confirmUnban"
            };
            
            boolean needsUpdate = false;
            debugLog("Checking for missing i18n keys...");
            for (String key : requiredKeys) {
                if (!currentContent.contains(key + "=")) {
                    needsUpdate = true;
                    debugLog("i18n file missing key: " + key);
                } else {
                    debugLog("i18n file has key: " + key);
                }
            }
            
            if (needsUpdate) {
                debugLog("i18n file needs update, starting update process...");
                // Individual backup logic for current file has been removed, replaced by global backup at startup.
                
                // Re-save default file
                debugLog("Updating i18n file with new keys...");
                plugin.saveResource("i18n/messages_" + lang + ".properties", false);
                File tempFile = new File(plugin.getDataFolder(), "i18n/messages_" + lang + ".properties");
                if (tempFile.exists()) {
                    tempFile.renameTo(propFile);
                    debugLog("Updated i18n file: " + propFile.getName());
                }
            } else {
                debugLog("i18n file is up to date, no update needed");
            }
        } catch (Exception e) {
            debugLog("Error checking i18n update: " + e.getMessage());
        }
    }

    /**
     * Update email templates
     */
    private void patchEmailTemplates() {
        boolean autoUpdateEmail = plugin.getConfig().getBoolean("auto_update_email", true);
        if (!autoUpdateEmail) return;
        debugLog("Updating email templates...");
        File emailDir = new File(plugin.getDataFolder(), "email");
        File[] htmlFiles = emailDir.listFiles((dir, name) -> name.endsWith(".html"));
        if (htmlFiles == null || htmlFiles.length == 0) {
            debugLog("No email templates found, auto-completing default templates...");
            String[] languages = {"zh", "en"};
            for (String lang : languages) {
                File templateFile = new File(emailDir, "verify_code_" + lang + ".html");
                if (!templateFile.exists()) {
                    try (InputStream in = plugin.getResource("email/verify_code_" + lang + ".html");
                         OutputStream out = new FileOutputStream(templateFile)) {
                        if (in != null) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = in.read(buffer)) != -1) {
                                out.write(buffer, 0, len);
                            }
                            debugLog("Created default email template: " + templateFile.getName());
                        } else {
                            debugLog("Default template not found in JAR: email/verify_code_" + lang + ".html");
                        }
                    } catch (Exception e) {
                        debugLog("Failed to create email template " + lang + ": " + e.getMessage());
                    }
                }
            }
        } else {
            debugLog("Email templates already exist, skipping auto-completion.");
        }
        // Backup and update built-in files
        File zh = new File(emailDir, "verify_code_zh.html");
        File en = new File(emailDir, "verify_code_en.html");
        if (!zh.exists()) {
            plugin.saveResource("email/verify_code_zh.html", false);
            plugin.getLogger().info("[VerifyMC] Email templates auto-patched");
        }
        if (!en.exists()) {
            plugin.saveResource("email/verify_code_en.html", false);
            plugin.getLogger().info("[VerifyMC] Email templates auto-patched");
        }
    }

    /**
     * Update static files
     */
    private void updateStaticThemes() {
        boolean autoUpdateStatic = plugin.getConfig().getBoolean("auto_update_static", true);
        if (!autoUpdateStatic) return;
        debugLog("Updating static files...");

        File staticDir = new File(plugin.getDataFolder(), "static");
        String[] themes = {"default", "glassx"};

        for (String theme : themes) {
            File themeDir = new File(staticDir, theme);
            if (!themeDir.exists()) {
                themeDir.mkdirs();
                debugLog("Created theme directory: " + theme);
            }

            if (shouldUpdateThemeFiles(themeDir)) {
                debugLog("Theme resources require update: " + theme);
                extractStaticFiles(themeDir, theme, true);
            } else {
                debugLog("Theme resources are up to date: " + theme);
            }
        }

        plugin.getLogger().info("[VerifyMC] Frontend static resources auto-updated");
    }

    /**
     * Check whether a theme directory is missing key frontend files.
     */
    private boolean shouldUpdateThemeFiles(File themeDir) {
        File[] files = themeDir.listFiles();
        if (files == null || files.length == 0) {
            return true;
        }

        String[] keyFiles = {"index.html", "assets", "css", "js"};
        for (String keyFile : keyFiles) {
            if (!new File(themeDir, keyFile).exists()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extract static files from jar package
     * @param themeDir Theme directory
     * @param theme Theme name
     * @param overwrite Whether to overwrite existing files
     */
    private void extractStaticFiles(File themeDir, String theme, boolean overwrite) {
        debugLog("Extracting static files for theme: " + theme);
        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            debugLog("Opened jar file: " + jarFile.getAbsolutePath());
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                int extractedCount = 0;
                int totalEntries = 0;
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    totalEntries++;
                    if (entryName.startsWith("static/" + theme + "/") && !entry.isDirectory()) {
                        String relativePath = entryName.substring(("static/" + theme + "/").length());
                        File outFile = new File(themeDir, relativePath);
                        debugLog("Extracting: " + entryName + " -> " + outFile.getAbsolutePath());
                        outFile.getParentFile().mkdirs();
                        if (overwrite || !outFile.exists()) {
                            try (InputStream in = jar.getInputStream(entry);
                                 OutputStream out = new FileOutputStream(outFile)) {
                                byte[] buffer = new byte[4096];
                                int len;
                                while ((len = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, len);
                                }
                                extractedCount++;
                            }
                        } else {
                            debugLog("Skipping extraction for " + outFile.getAbsolutePath() + " (already exists)");
                        }
                    }
                }
                debugLog("Scanned " + totalEntries + " jar entries, extracted " + extractedCount + " files for theme: " + theme);
            }
        } catch (Exception e) {
            debugLog("Failed to extract static files for theme " + theme + ": " + e.getMessage());
        }
    }

    /**
     * Load i18n resource bundle for specified language
     * @param lang Language code (e.g. "zh", "en", "ja", etc.)
     * @return ResourceBundle for the specified language
     */
    public ResourceBundle loadI18nBundle(String lang) {
        debugLog("Loading i18n bundle for language: " + lang);
        
        try {
            File i18nDir = new File(plugin.getDataFolder(), "i18n");
            debugLog("i18n directory: " + i18nDir.getAbsolutePath());
            
            File propFile = new File(i18nDir, "messages_" + lang + ".properties");
            
            // If language file doesn't exist, create it from English template
            if (!propFile.exists()) {
                debugLog("Language file not found: messages_" + lang + ".properties");
                createLanguageFile(lang);
            }
            
            // Load language resource bundle
            if (propFile.exists()) {
                debugLog("Loading external i18n bundle: " + propFile.getAbsolutePath());
                try (InputStreamReader reader = new InputStreamReader(new FileInputStream(propFile), StandardCharsets.UTF_8)) {
                    ResourceBundle messages = new PropertyResourceBundle(reader);
                    debugLog("Successfully loaded external i18n bundle for language: " + lang);
                    return messages;
                }
            } else {
                debugLog("External i18n bundle not found, loading internal bundle");
                try {
                    ResourceBundle messages = ResourceBundle.getBundle("i18n.messages", new Locale(lang));
                    debugLog("Loaded internal i18n bundle for language: " + lang);
                    return messages;
                } catch (Exception e) {
                    debugLog("Internal bundle not found for language: " + lang + ", falling back to English");
                    return ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("[VerifyMC] Failed to load i18n bundles: " + e.getMessage());
            debugLog("Failed to load i18n bundle: " + e.getMessage());
            
            // Use internal English resource bundle as fallback
            debugLog("Falling back to internal English i18n bundle");
            return ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
        }
    }
    
    /**
     * Create language file from English template
     * @param lang Language code
     */
    private void createLanguageFile(String lang) {
        debugLog("Creating language file for: " + lang);
        try {
            File i18nDir = new File(plugin.getDataFolder(), "i18n");
            File targetFile = new File(i18nDir, "messages_" + lang + ".properties");
            File enTemplate = new File(i18nDir, "messages_en.properties");
            
            // If English template exists, copy it
            if (enTemplate.exists()) {
                Files.copy(enTemplate.toPath(), targetFile.toPath());
                plugin.getLogger().info("[VerifyMC] Auto-created language file: messages_" + lang + ".properties (based on English template)");
                debugLog("Created language file from English template: messages_" + lang + ".properties");
            } else {
                // Try to get English template from JAR
                try (InputStream in = plugin.getResource("i18n/messages_en.properties");
                     OutputStream out = new FileOutputStream(targetFile)) {
                    if (in != null) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                        plugin.getLogger().info("[VerifyMC] Auto-created language file: messages_" + lang + ".properties (based on English template)");
                        debugLog("Created language file from JAR template: messages_" + lang + ".properties");
                    }
                }
            }
        } catch (Exception e) {
            debugLog("Failed to create language file for " + lang + ": " + e.getMessage());
        }
    }

    /**
     * Get theme static file directory
     * @param theme Theme name
     * @return Theme static directory path
     */
    public String getThemeStaticDir(String theme) {
        File themeDir = new File(plugin.getDataFolder(), "static/" + theme);
        return themeDir.getAbsolutePath();
    }

    /**
     * Check if theme exists
     * @param theme Theme name
     * @return true if theme exists
     */
    public boolean themeExists(String theme) {
        File themeDir = new File(plugin.getDataFolder(), "static/" + theme);
        return themeDir.exists() && themeDir.isDirectory();
    }
} 