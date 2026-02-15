package team.kitemc.verifymc.infrastructure.exception;

public enum ErrorCode {
    VALIDATION_ERROR("VAL_001", "Parameter validation failed", 400),
    USER_NOT_FOUND("USR_001", "User not found", 404),
    USER_ALREADY_EXISTS("USR_002", "User already exists", 409),
    EMAIL_ALREADY_USED("USR_003", "Email already in use", 409),
    INVALID_VERIFICATION_CODE("VER_001", "Invalid verification code", 400),
    VERIFICATION_CODE_EXPIRED("VER_002", "Verification code has expired", 400),
    RATE_LIMIT_EXCEEDED("RATE_001", "Request rate limit exceeded", 429),
    DISCORD_NOT_LINKED("DIS_001", "Discord account not linked", 400),
    DISCORD_ALREADY_LINKED("DIS_002", "Discord account already linked", 409),
    QUESTIONNAIRE_REQUIRED("QNR_001", "Questionnaire is required", 400),
    QUESTIONNAIRE_NOT_PASSED("QNR_002", "Questionnaire not passed", 400),
    AUTHME_INTEGRATION_ERROR("AUTH_001", "AuthMe integration error", 500),
    UNAUTHORIZED("AUTH_002", "Authentication required", 401),
    CONFIGURATION_ERROR("CFG_001", "Configuration error", 500),
    INTERNAL_ERROR("SYS_001", "Internal server error", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
