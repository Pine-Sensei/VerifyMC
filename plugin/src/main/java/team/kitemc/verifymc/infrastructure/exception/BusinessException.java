package team.kitemc.verifymc.infrastructure.exception;

import java.util.Map;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String messageKey;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.messageKey = null;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.messageKey = null;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.messageKey = null;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.messageKey = null;
        this.details = details;
    }

    public BusinessException(ErrorCode errorCode, String message, String messageKey) {
        super(message);
        this.errorCode = errorCode;
        this.messageKey = messageKey;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message, String messageKey, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.messageKey = messageKey;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public String getErrorCodeString() {
        return errorCode.getCode();
    }

    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }
}
