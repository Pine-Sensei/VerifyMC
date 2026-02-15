package team.kitemc.verifymc.domain.service;

public interface ConfigurationService {
    boolean getBoolean(String key, boolean defaultValue);

    int getInt(String key, int defaultValue);

    long getLong(String key, long defaultValue);

    String getString(String key, String defaultValue);

    String getString(String key);

    boolean isDebug();

    String getLanguage();

    void reload();

    String getWebServerPrefix();

    java.io.File getDataFolder();

    java.util.logging.Logger getLogger();
}
