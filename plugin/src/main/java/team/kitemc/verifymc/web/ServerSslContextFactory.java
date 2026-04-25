package team.kitemc.verifymc.web;

import team.kitemc.verifymc.core.ConfigManager;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Locale;

/**
 * Loads a shared SSLContext from the configured keystore for HTTPS and WSS.
 */
public final class ServerSslContextFactory {
    private ServerSslContextFactory() {
    }

    public static SSLContext create(ConfigManager config)
            throws IOException, GeneralSecurityException {
        Path keystorePath = config.resolveSslKeystorePath();
        String keystoreType = normalizeKeystoreType(config.getSslKeystoreType());
        char[] password = config.getSslKeystorePassword().toCharArray();

        try (InputStream inputStream = Files.newInputStream(keystorePath)) {
            KeyStore keyStore = KeyStore.getInstance(keystoreType);
            keyStore.load(inputStream, password);

            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                    keyManagerFactory.getKeyManagers(),
                    trustManagerFactory.getTrustManagers(),
                    null);
            return sslContext;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public static Path resolveKeystorePath(Path dataDirectory, String configuredPath) throws IOException {
        String trimmedPath = configuredPath == null ? "" : configuredPath.trim();
        if (trimmedPath.isEmpty()) {
            throw new IOException("ssl.keystore.path is empty");
        }

        Path path;
        try {
            path = Path.of(trimmedPath);
        } catch (InvalidPathException e) {
            throw new IOException("Invalid keystore path: " + trimmedPath, e);
        }

        if (!path.isAbsolute()) {
            path = dataDirectory.resolve(path);
        }

        path = path.normalize();
        if (!Files.isRegularFile(path)) {
            throw new IOException("Keystore file not found: " + path);
        }

        return path;
    }

    static String normalizeKeystoreType(String configuredType) {
        if (configuredType == null || configuredType.trim().isEmpty()) {
            return "PKCS12";
        }
        return configuredType.trim().toUpperCase(Locale.ROOT);
    }
}
