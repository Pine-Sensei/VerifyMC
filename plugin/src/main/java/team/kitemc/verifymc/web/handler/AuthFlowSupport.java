package team.kitemc.verifymc.web.handler;

import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import team.kitemc.verifymc.core.ConfigManager;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.util.EmailAddressUtil;
import team.kitemc.verifymc.util.PhoneNumberUtil;

public final class AuthFlowSupport {
    private AuthFlowSupport() {
    }

    public static ConfigManager.VerifyIdentifier parseIdentifier(String value) {
        if (value == null) {
            return ConfigManager.VerifyIdentifier.USERNAME;
        }
        return switch (value.trim().toLowerCase()) {
            case "email" -> ConfigManager.VerifyIdentifier.EMAIL;
            case "phone", "sms" -> ConfigManager.VerifyIdentifier.PHONE;
            default -> ConfigManager.VerifyIdentifier.USERNAME;
        };
    }

    public static String normalizeIdentifier(ConfigManager.VerifyIdentifier identifier, String raw, String countryCode) {
        return switch (identifier) {
            case EMAIL -> EmailAddressUtil.normalize(raw);
            case PHONE -> normalizePhone(countryCode, raw);
            case USERNAME -> raw == null ? "" : raw.trim();
        };
    }

    public static boolean missingRequiredCountryCode(String raw, String countryCode) {
        String phone = raw == null ? "" : raw.trim();
        return !phone.startsWith("+") && (countryCode == null || countryCode.trim().isEmpty());
    }

    public static String normalizePhone(String countryCode, String phone) {
        String raw = phone == null ? "" : phone.trim();
        if (raw.startsWith("+") || raw.startsWith("00")) {
            return PhoneNumberUtil.normalize(raw);
        }
        String code = countryCode == null ? "" : countryCode.trim();
        if (!code.isEmpty()) {
            code = code.startsWith("+") ? code.substring(1) : code;
            raw = "+" + code + raw;
        }
        return PhoneNumberUtil.normalize(raw);
    }

    public static List<Map<String, Object>> findUsers(UserDao userDao, ConfigManager.VerifyIdentifier identifier, String value) {
        return switch (identifier) {
            case EMAIL -> userDao.getUsersByEmail(value);
            case PHONE -> userDao.getUsersByPhone(value);
            case USERNAME -> {
                Map<String, Object> user = userDao.getUserByUsername(value);
                yield user == null ? List.of() : List.of(user);
            }
        };
    }

    public static JSONArray accountSummaries(List<Map<String, Object>> users) {
        JSONArray accounts = new JSONArray();
        for (Map<String, Object> user : users) {
            JSONObject account = new JSONObject();
            account.put("username", String.valueOf(user.getOrDefault("username", "")));
            account.put("status", String.valueOf(user.getOrDefault("status", "")));
            Object email = user.get("email");
            Object phone = user.get("phone");
            if (email != null && !email.toString().isBlank()) {
                account.put("email", maskEmail(email.toString()));
            }
            if (phone != null && !phone.toString().isBlank()) {
                account.put("phone", maskPhone(phone.toString()));
            }
            accounts.put(account);
        }
        return accounts;
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private static String maskPhone(String phone) {
        if (phone.length() <= 6) {
            return "***";
        }
        return phone.substring(0, Math.min(3, phone.length())) + "****" + phone.substring(phone.length() - 3);
    }
}
