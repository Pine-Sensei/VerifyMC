package team.kitemc.verifymc.domain.service;

import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;

public final class RegistrationResult {
    private final boolean success;
    private final UserStatus status;
    private final String message;
    private final String messageKey;
    private final User user;

    private RegistrationResult(Builder builder) {
        this.success = builder.success;
        this.status = builder.status;
        this.message = builder.message;
        this.messageKey = builder.messageKey;
        this.user = builder.user;
    }

    public boolean isSuccess() {
        return success;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public User getUser() {
        return user;
    }

    public boolean isWhitelisted() {
        return success && status == UserStatus.APPROVED;
    }

    public boolean isPendingReview() {
        return success && status == UserStatus.PENDING;
    }

    public static RegistrationResult success(User user, UserStatus status, String message) {
        return new Builder()
                .success(true)
                .status(status)
                .message(message)
                .user(user)
                .build();
    }

    public static RegistrationResult success(User user, UserStatus status, String message, String messageKey) {
        return new Builder()
                .success(true)
                .status(status)
                .message(message)
                .messageKey(messageKey)
                .user(user)
                .build();
    }

    public static RegistrationResult failure(String message) {
        return new Builder()
                .success(false)
                .status(null)
                .message(message)
                .build();
    }

    public static RegistrationResult failure(String message, String messageKey) {
        return new Builder()
                .success(false)
                .status(null)
                .message(message)
                .messageKey(messageKey)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private UserStatus status;
        private String message;
        private String messageKey;
        private User user;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder messageKey(String messageKey) {
            this.messageKey = messageKey;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public RegistrationResult build() {
            return new RegistrationResult(this);
        }
    }
}
