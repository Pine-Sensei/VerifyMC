package team.kitemc.verifymc.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AccountSelectionService {
    private static final String TOKEN_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int TOKEN_LENGTH = 32;

    private final ConcurrentHashMap<String, SelectionEntry> selections = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final long expireMillis;

    public AccountSelectionService(long expireMillis) {
        this.expireMillis = Math.max(1L, expireMillis);
    }

    public String issueToken(Purpose purpose, String identifierType, String identifier, Collection<String> usernames) {
        cleanupExpiredSelections();
        Set<String> normalizedUsers = new LinkedHashSet<>();
        if (usernames != null) {
            for (String username : usernames) {
                if (username != null && !username.isBlank()) {
                    normalizedUsers.add(username);
                }
            }
        }
        if (normalizedUsers.isEmpty()) {
            throw new IllegalArgumentException("At least one username is required");
        }

        String token = generateToken();
        selections.put(token, new SelectionEntry(
                purpose,
                identifierType == null ? "" : identifierType,
                identifier == null ? "" : identifier,
                new ArrayList<>(normalizedUsers),
                System.currentTimeMillis() + expireMillis));
        return token;
    }

    public ConsumeResult consume(String token, Purpose purpose, String selectedUsername) {
        cleanupExpiredSelections();
        if (token == null || token.isBlank() || selectedUsername == null || selectedUsername.isBlank()) {
            return ConsumeResult.invalid();
        }

        SelectionEntry entry = selections.remove(token);
        if (entry == null || entry.expireAt < System.currentTimeMillis() || entry.purpose != purpose) {
            return ConsumeResult.invalid();
        }

        String expected = selectedUsername.trim();
        for (String username : entry.usernames) {
            if (username.equals(expected)) {
                return ConsumeResult.success(entry, username);
            }
        }
        return ConsumeResult.invalid();
    }

    public void cleanupExpiredSelections() {
        long now = System.currentTimeMillis();
        selections.entrySet().removeIf(entry -> entry.getValue().expireAt < now);
    }

    private String generateToken() {
        StringBuilder builder = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            builder.append(TOKEN_CHARS.charAt(secureRandom.nextInt(TOKEN_CHARS.length())));
        }
        return builder.toString();
    }

    public enum Purpose {
        LOGIN,
        FORGOT_PASSWORD
    }

    public record SelectionEntry(
            Purpose purpose,
            String identifierType,
            String identifier,
            List<String> usernames,
            long expireAt) {
    }

    public record ConsumeResult(boolean valid, SelectionEntry entry, String username) {
        public static ConsumeResult success(SelectionEntry entry, String username) {
            return new ConsumeResult(true, entry, username);
        }

        public static ConsumeResult invalid() {
            return new ConsumeResult(false, null, null);
        }
    }
}
