package team.kitemc.verifymc.web.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class SecurityHelper {
    private static final SecureRandom RANDOM = new SecureRandom();
    
    public static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String combined = password + salt;
            byte[] hash = md.digest(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    public static boolean verifyPassword(String password, String salt, String hash) {
        String computed = hashPassword(password, salt);
        return computed.equals(hash);
    }
    
    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }
    
    public static boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) return false;
        try {
            Base64.getUrlDecoder().decode(token);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
