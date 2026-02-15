package team.kitemc.verifymc.domain.model;

public enum UserStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    BANNED("banned");

    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserStatus fromString(String value) {
        if (value == null) {
            return PENDING;
        }
        for (UserStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return PENDING;
    }
}
