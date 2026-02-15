package team.kitemc.verifymc.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ConfigValidator {

    private final List<ValidationError> errors = new ArrayList<>();

    public static class ValidationError {
        private final String path;
        private final String message;
        private final ValidationErrorType type;

        public ValidationError(String path, String message, ValidationErrorType type) {
            this.path = path;
            this.message = message;
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public String getMessage() {
            return message;
        }

        public ValidationErrorType getType() {
            return type;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s", type, path, message);
        }
    }

    public enum ValidationErrorType {
        MISSING_REQUIRED,
        INVALID_FORMAT,
        OUT_OF_RANGE,
        INVALID_TYPE,
        EMPTY_VALUE
    }

    public ConfigValidator require(String path, Object value) {
        if (value == null) {
            errors.add(new ValidationError(path, "Required configuration value is missing", ValidationErrorType.MISSING_REQUIRED));
        }
        return this;
    }

    public ConfigValidator requireNonEmpty(String path, String value) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(new ValidationError(path, "Required configuration value is missing or empty", ValidationErrorType.EMPTY_VALUE));
        }
        return this;
    }

    public ConfigValidator validateRegex(String path, String value, String pattern, String description) {
        if (value != null && !value.isEmpty()) {
            if (!Pattern.matches(pattern, value)) {
                errors.add(new ValidationError(path, 
                    String.format("Value '%s' does not match expected format: %s", value, description), 
                    ValidationErrorType.INVALID_FORMAT));
            }
        }
        return this;
    }

    public ConfigValidator validateEmail(String path, String email) {
        if (email != null && !email.isEmpty()) {
            String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
            if (!Pattern.matches(emailPattern, email)) {
                errors.add(new ValidationError(path, 
                    String.format("'%s' is not a valid email address", email), 
                    ValidationErrorType.INVALID_FORMAT));
            }
        }
        return this;
    }

    public ConfigValidator validateUrl(String path, String url) {
        if (url != null && !url.isEmpty()) {
            String urlPattern = "^(https?://)[\\w.-]+(?:\\.[\\w.-]+)+[/#?]?.*$";
            if (!Pattern.matches(urlPattern, url)) {
                errors.add(new ValidationError(path, 
                    String.format("'%s' is not a valid URL", url), 
                    ValidationErrorType.INVALID_FORMAT));
            }
        }
        return this;
    }

    public ConfigValidator validateRange(String path, int value, int min, int max) {
        if (value < min || value > max) {
            errors.add(new ValidationError(path, 
                String.format("Value %d is out of range [%d, %d]", value, min, max), 
                ValidationErrorType.OUT_OF_RANGE));
        }
        return this;
    }

    public ConfigValidator validateRange(String path, long value, long min, long max) {
        if (value < min || value > max) {
            errors.add(new ValidationError(path, 
                String.format("Value %d is out of range [%d, %d]", value, min, max), 
                ValidationErrorType.OUT_OF_RANGE));
        }
        return this;
    }

    public ConfigValidator validateRange(String path, double value, double min, double max) {
        if (value < min || value > max) {
            errors.add(new ValidationError(path, 
                String.format("Value %f is out of range [%f, %f]", value, min, max), 
                ValidationErrorType.OUT_OF_RANGE));
        }
        return this;
    }

    public ConfigValidator validatePositive(String path, int value) {
        if (value <= 0) {
            errors.add(new ValidationError(path, 
                String.format("Value %d must be positive", value), 
                ValidationErrorType.OUT_OF_RANGE));
        }
        return this;
    }

    public ConfigValidator validatePositive(String path, long value) {
        if (value <= 0) {
            errors.add(new ValidationError(path, 
                String.format("Value %d must be positive", value), 
                ValidationErrorType.OUT_OF_RANGE));
        }
        return this;
    }

    public ConfigValidator validatePort(String path, int port) {
        if (port < 1 || port > 65535) {
            errors.add(new ValidationError(path, 
                String.format("Port %d is not valid (must be 1-65535)", port), 
                ValidationErrorType.OUT_OF_RANGE));
        }
        return this;
    }

    public ConfigValidator validateInList(String path, String value, List<String> allowedValues) {
        if (value != null && !allowedValues.contains(value)) {
            errors.add(new ValidationError(path, 
                String.format("Value '%s' is not one of allowed values: %s", value, allowedValues), 
                ValidationErrorType.INVALID_FORMAT));
        }
        return this;
    }

    public ConfigValidator validateInList(String path, Object value, Object[] allowedValues) {
        if (value != null) {
            boolean found = false;
            for (Object allowed : allowedValues) {
                if (value.equals(allowed)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                errors.add(new ValidationError(path, 
                    String.format("Value '%s' is not one of allowed values", value), 
                    ValidationErrorType.INVALID_FORMAT));
            }
        }
        return this;
    }

    public ConfigValidator custom(String path, boolean isValid, String message) {
        if (!isValid) {
            errors.add(new ValidationError(path, message, ValidationErrorType.INVALID_FORMAT));
        }
        return this;
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }

    public String getErrorMessage() {
        StringBuilder sb = new StringBuilder();
        for (ValidationError error : errors) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(error.toString());
        }
        return sb.toString();
    }

    public void throwIfInvalid() throws ConfigValidationException {
        if (hasErrors()) {
            throw new ConfigValidationException(getErrorMessage(), errors);
        }
    }

    public static class ConfigValidationException extends Exception {
        private final List<ValidationError> errors;

        public ConfigValidationException(String message, List<ValidationError> errors) {
            super(message);
            this.errors = errors;
        }

        public List<ValidationError> getErrors() {
            return errors;
        }
    }
}
