package team.kitemc.verifymc.infrastructure.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationException extends BusinessException {
    private final List<FieldError> fieldErrors;

    public ValidationException(String fieldName, String errorMessage) {
        super(ErrorCode.VALIDATION_ERROR, errorMessage);
        this.fieldErrors = Collections.singletonList(new FieldError(fieldName, errorMessage));
    }

    public ValidationException(List<FieldError> fieldErrors) {
        super(ErrorCode.VALIDATION_ERROR, buildMessage(fieldErrors));
        this.fieldErrors = new ArrayList<>(fieldErrors);
    }

    public ValidationException(String message, List<FieldError> fieldErrors) {
        super(ErrorCode.VALIDATION_ERROR, message);
        this.fieldErrors = new ArrayList<>(fieldErrors);
    }

    public List<FieldError> getFieldErrors() {
        return Collections.unmodifiableList(fieldErrors);
    }

    public void addFieldError(String fieldName, String errorMessage) {
        fieldErrors.add(new FieldError(fieldName, errorMessage));
    }

    private static String buildMessage(List<FieldError> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Validation failed";
        }
        StringBuilder sb = new StringBuilder("Validation failed: ");
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            FieldError error = errors.get(i);
            sb.append(error.getFieldName()).append(" ").append(error.getErrorMessage());
        }
        return sb.toString();
    }

    public static class FieldError {
        private final String fieldName;
        private final String errorMessage;

        public FieldError(String fieldName, String errorMessage) {
            this.fieldName = fieldName;
            this.errorMessage = errorMessage;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
