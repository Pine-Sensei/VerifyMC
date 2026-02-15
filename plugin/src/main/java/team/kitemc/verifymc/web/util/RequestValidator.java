package team.kitemc.verifymc.web.util;

import java.util.regex.Pattern;
import java.util.UUID;

public class RequestValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,16}$");
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    public static boolean isValidUsername(String username, Pattern customPattern) {
        if (username == null || username.isEmpty()) return false;
        Pattern pattern = customPattern != null ? customPattern : USERNAME_PATTERN;
        return pattern.matcher(username).matches();
    }
    
    public static boolean isValidUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) return false;
        return UUID_PATTERN.matcher(uuid).matches();
    }
    
    public static boolean isValidPassword(String password, Pattern passwordPattern) {
        if (password == null || password.isEmpty()) return false;
        if (passwordPattern == null) return true;
        return passwordPattern.matcher(password).matches();
    }
    
    public static boolean isEmailAlias(String email) {
        return email != null && email.contains("+");
    }
    
    public static String getEmailDomain(String email) {
        if (email == null || !email.contains("@")) return null;
        return email.substring(email.indexOf("@") + 1).toLowerCase();
    }
    
    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }
    
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public static boolean isPositive(int value) {
        return value > 0;
    }
    
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
}
