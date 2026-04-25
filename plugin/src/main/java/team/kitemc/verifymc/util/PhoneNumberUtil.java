package team.kitemc.verifymc.util;

public final class PhoneNumberUtil {
    private PhoneNumberUtil() {}

    public static String normalize(String phone) {
        if (phone == null) {
            return "";
        }
        String trimmed = phone.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        StringBuilder digits = new StringBuilder();
        boolean hasPlus = trimmed.startsWith("+");
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        if (digits.length() == 0) {
            return "";
        }
        String value = digits.toString();
        if (value.startsWith("00")) {
            value = value.substring(2);
            hasPlus = true;
        }
        if (!hasPlus && value.length() == 11 && value.startsWith("1")) {
            value = "86" + value;
        }
        if (!value.startsWith("86") && !hasPlus) {
            return "";
        }
        return "+" + value;
    }

    public static boolean isValid(String phone) {
        String normalized = normalize(phone);
        if (!normalized.startsWith("+")) {
            return false;
        }
        String digits = normalized.substring(1);
        return digits.length() >= 8 && digits.length() <= 15 && digits.matches("\\d+");
    }

    public static String toAliyunPhoneNumber(String phone) {
        String normalized = normalize(phone);
        if (normalized.startsWith("+86") && normalized.length() == 14) {
            return normalized.substring(3);
        }
        return normalized.startsWith("+") ? normalized.substring(1) : normalized;
    }
}
