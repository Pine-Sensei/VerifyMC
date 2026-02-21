package team.kitemc.verifymc.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class PasswordUtil {
    private static final int SALT_LENGTH = 16;
    private static final String HASH_PREFIX = "$SHA$";
    private static final String DELIMITER = "$";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (isHashed(plainPassword)) {
            return plainPassword;
        }
        String salt = generateHexSalt();
        String hash = sha256Hex(sha256Hex(plainPassword) + salt);
        return HASH_PREFIX + salt + DELIMITER + hash;
    }

    public static boolean verify(String plainPassword, String storedPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return false;
        }
        if (storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }

        if (storedPassword.startsWith(HASH_PREFIX)) {
            return verifySaltedHash(plainPassword, storedPassword);
        }

        if (isUnsaltedSha256(storedPassword)) {
            String computedHash = sha256Hex(plainPassword);
            return computedHash.equalsIgnoreCase(storedPassword);
        }

        return plainPassword.equals(storedPassword);
    }

    private static boolean verifySaltedHash(String plainPassword, String storedPassword) {
        String[] parts = storedPassword.split("\\" + DELIMITER);
        if (parts.length < 3) {
            return false;
        }
        String salt = parts[1];
        String storedHash = parts[2];
        String computedHash = sha256Hex(sha256Hex(plainPassword) + salt);
        return computedHash.equals(storedHash);
    }

    public static boolean isHashed(String password) {
        return password != null && password.startsWith(HASH_PREFIX);
    }

    private static boolean isUnsaltedSha256(String password) {
        if (password == null || password.length() != 64) {
            return false;
        }
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    private static String generateHexSalt() {
        StringBuilder sb = new StringBuilder(SALT_LENGTH);
        for (int i = 0; i < SALT_LENGTH; i++) {
            sb.append(Integer.toHexString(SECURE_RANDOM.nextInt(16)));
        }
        return sb.toString();
    }

    private static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
