package team.kitemc.verifymc.security;

public enum AdminAuthMode {
    OP,
    PERMISSION;

    public static AdminAuthMode fromConfig(String rawValue) {
        if (rawValue == null) {
            return OP;
        }
        return "permission".equalsIgnoreCase(rawValue.trim()) ? PERMISSION : OP;
    }
}
