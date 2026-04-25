package team.kitemc.verifymc.web.handler;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import team.kitemc.verifymc.core.ConfigManager;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.db.AuditRecord;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.service.AuthmeService;
import team.kitemc.verifymc.util.PasswordUtil;
import team.kitemc.verifymc.util.EmailAddressUtil;
import team.kitemc.verifymc.util.PhoneNumberUtil;
import team.kitemc.verifymc.web.ApiResponseFactory;

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
        return !phone.startsWith("+") && !phone.startsWith("00")
                && (countryCode == null || countryCode.trim().isEmpty());
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

    public static JSONObject accountSelectionRequiredResponse(PluginContext ctx,
            team.kitemc.verifymc.service.AccountSelectionService.Purpose purpose,
            ConfigManager.VerifyIdentifier identifierType,
            String identifier,
            List<Map<String, Object>> users,
            String language) {
        String selectionToken = ctx.getAccountSelectionService().issueToken(
                purpose,
                identifierType.configPrefix(),
                identifier,
                usernames(users));
        JSONObject response = ApiResponseFactory.failure(ctx.getMessage("login.account_selection_required", language));
        response.put("code", "ACCOUNT_SELECTION_REQUIRED");
        response.put("accounts", accountSummaries(users));
        response.put("selectionToken", selectionToken);
        return response;
    }

    public static Map<String, Object> selectUser(List<Map<String, Object>> users, String selectedUsername, boolean usernameCaseSensitive) {
        if (users.size() == 1 && (selectedUsername == null || selectedUsername.isBlank())) {
            return users.get(0);
        }
        String expected = selectedUsername == null ? "" : selectedUsername.trim();
        for (Map<String, Object> user : users) {
            Object username = user.get("username");
            if (username == null) {
                continue;
            }
            String actual = username.toString();
            if (usernameCaseSensitive ? actual.equals(expected) : actual.equalsIgnoreCase(expected)) {
                return user;
            }
        }
        return null;
    }

    public static Map<String, Object> verifyPasswordAgainstUsers(PluginContext ctx, List<Map<String, Object>> users, String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        for (Map<String, Object> user : users) {
            String username = String.valueOf(user.getOrDefault("username", ""));
            String storedPassword = String.valueOf(user.getOrDefault("password", ""));
            if (verifyPassword(ctx, username, password, storedPassword)) {
                return user;
            }
        }
        return null;
    }

    public static boolean verifyPassword(PluginContext ctx, String username, String password, String storedPassword) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return false;
        }

        AuthmeService authmeService = ctx.getAuthmeService();
        boolean passwordValid = false;

        if (authmeService != null && authmeService.isAuthmeEnabled() && authmeService.hasAuthmeUser(username)) {
            String authmePassword = authmeService.getAuthmePassword(username);
            if (authmePassword != null && !authmePassword.isEmpty()) {
                passwordValid = PasswordUtil.verify(password, authmePassword);
            }
        }

        if (!passwordValid && storedPassword != null && !storedPassword.isEmpty()) {
            passwordValid = PasswordUtil.verify(password, storedPassword);
        }
        return passwordValid;
    }

    public static SharedPasswordUpdateResult synchronizeSharedPasswords(PluginContext ctx, List<Map<String, Object>> users, String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return SharedPasswordUpdateResult.skippedResult();
        }
        List<String> usernames = usernames(users);
        if (usernames.isEmpty()) {
            return SharedPasswordUpdateResult.failure(List.of());
        }
        if (!ctx.getUserDao().updateSharedPasswords(usernames, plainPassword)) {
            return SharedPasswordUpdateResult.failure(usernames);
        }

        if (ctx.getAuthmeService() != null && ctx.getAuthmeService().isAuthmeEnabled()) {
            for (String username : usernames) {
                if (!ctx.getAuthmeService().syncUserPasswordToAuthme(username, plainPassword)) {
                    ctx.getPlugin().getLogger().warning("[VerifyMC] Failed to sync shared password to AuthMe for user: " + username);
                    recordAuthmeSyncFailure(ctx, username, usernames);
                }
            }
        }
        return SharedPasswordUpdateResult.success(usernames);
    }

    public static List<Map<String, Object>> resolveSharedPasswordGroup(UserDao userDao, Map<String, Object> seedUser) {
        if (seedUser == null) {
            return List.of();
        }
        List<Map<String, Object>> sharedUsers = collectSharedPasswordUsers(userDao, seedUser);
        return sharedUsers.isEmpty() ? List.of(seedUser) : sharedUsers;
    }

    public static List<Map<String, Object>> collectSharedPasswordUsers(UserDao userDao, Map<String, Object> seedUser) {
        LinkedHashMap<String, Map<String, Object>> users = new LinkedHashMap<>();
        addUser(users, seedUser);
        if (seedUser == null) {
            return List.of();
        }

        Object email = seedUser.get("email");
        if (email != null && !email.toString().isBlank()) {
            for (Map<String, Object> user : userDao.getUsersByEmail(email.toString())) {
                addUser(users, user);
            }
        }

        Object phone = seedUser.get("phone");
        if (phone != null && !phone.toString().isBlank()) {
            for (Map<String, Object> user : userDao.getUsersByPhone(phone.toString())) {
                addUser(users, user);
            }
        }
        return List.copyOf(users.values());
    }

    public static List<String> usernames(List<Map<String, Object>> users) {
        Set<String> names = new LinkedHashSet<>();
        for (Map<String, Object> user : users) {
            Object username = user.get("username");
            if (username != null && !username.toString().isBlank()) {
                names.add(username.toString());
            }
        }
        return List.copyOf(names);
    }

    private static void addUser(Map<String, Map<String, Object>> users, Map<String, Object> user) {
        if (user == null) {
            return;
        }
        Object username = user.get("username");
        if (username == null || username.toString().isBlank()) {
            return;
        }
        users.putIfAbsent(username.toString(), user);
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

    private static void recordAuthmeSyncFailure(PluginContext ctx, String username, List<String> usernames) {
        if (ctx.getAuditDao() == null) {
            return;
        }
        ctx.getAuditDao().addAudit(new AuditRecord(
                "authme_password_sync_failed",
                "system",
                username,
                "Failed to sync shared password to AuthMe for user " + username
                        + " within group [" + String.join(", ", usernames) + "]",
                System.currentTimeMillis()));
    }

    public record SharedPasswordUpdateResult(boolean success, boolean skipped, List<String> usernames) {
        public static SharedPasswordUpdateResult success(List<String> usernames) {
            return new SharedPasswordUpdateResult(true, false, List.copyOf(usernames));
        }

        public static SharedPasswordUpdateResult failure(List<String> usernames) {
            return new SharedPasswordUpdateResult(false, false, List.copyOf(usernames));
        }

        public static SharedPasswordUpdateResult skippedResult() {
            return new SharedPasswordUpdateResult(true, true, List.of());
        }
    }
}
