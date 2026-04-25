package team.kitemc.verifymc.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailAddressUtil {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    private EmailAddressUtil() {}

    public static String normalize(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String email) {
        String normalized = normalize(email);
        return !normalized.isBlank() && EMAIL_PATTERN.matcher(normalized).matches();
    }

    public static String extractDomain(String email) {
        String normalized = normalize(email);
        int atIndex = normalized.indexOf('@');
        if (atIndex < 0 || atIndex == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(atIndex + 1);
    }

    public static boolean hasAlias(String email) {
        String normalized = normalize(email);
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0) {
            return false;
        }
        return normalized.substring(0, atIndex).contains("+");
    }
}
