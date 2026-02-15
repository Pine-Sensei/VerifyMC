package team.kitemc.verifymc.infrastructure.web;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RequestValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final List<ValidationRule> rules = new ArrayList<>();

    public RequestValidator require(String field) {
        rules.add(new ValidationRule(field, RuleType.REQUIRED, null));
        return this;
    }

    public RequestValidator email(String field) {
        rules.add(new ValidationRule(field, RuleType.EMAIL, null));
        return this;
    }

    public RequestValidator uuid(String field) {
        rules.add(new ValidationRule(field, RuleType.UUID, null));
        return this;
    }

    public RequestValidator minLength(String field, int min) {
        rules.add(new ValidationRule(field, RuleType.MIN_LENGTH, min));
        return this;
    }

    public RequestValidator maxLength(String field, int max) {
        rules.add(new ValidationRule(field, RuleType.MAX_LENGTH, max));
        return this;
    }

    public RequestValidator pattern(String field, String regex) {
        rules.add(new ValidationRule(field, RuleType.PATTERN, Pattern.compile(regex)));
        return this;
    }

    public RequestValidator pattern(String field, Pattern pattern) {
        rules.add(new ValidationRule(field, RuleType.PATTERN, pattern));
        return this;
    }

    public RequestValidator min(String field, Number min) {
        rules.add(new ValidationRule(field, RuleType.MIN, min));
        return this;
    }

    public RequestValidator max(String field, Number max) {
        rules.add(new ValidationRule(field, RuleType.MAX, max));
        return this;
    }

    public RequestValidator type(String field, Class<?> type) {
        rules.add(new ValidationRule(field, RuleType.TYPE, type));
        return this;
    }

    public RequestValidator custom(String field, String errorMessage, ValidationPredicate predicate) {
        rules.add(new ValidationRule(field, RuleType.CUSTOM, new CustomValidation(predicate, errorMessage)));
        return this;
    }

    public ValidationResult validate(JSONObject json) {
        if (json == null) {
            return new ValidationResult(List.of(new FieldError("body", "Request body is null")));
        }

        List<FieldError> errors = new ArrayList<>();

        for (ValidationRule rule : rules) {
            String field = rule.field;
            Object value = json.opt(field);

            switch (rule.type) {
                case REQUIRED:
                    if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                        errors.add(new FieldError(field, "Field is required"));
                    }
                    break;

                case EMAIL:
                    if (value != null && !value.toString().isEmpty()) {
                        if (!EMAIL_PATTERN.matcher(value.toString()).matches()) {
                            errors.add(new FieldError(field, "Invalid email format"));
                        }
                    }
                    break;

                case UUID:
                    if (value != null && !value.toString().isEmpty()) {
                        if (!UUID_PATTERN.matcher(value.toString()).matches()) {
                            errors.add(new FieldError(field, "Invalid UUID format"));
                        }
                    }
                    break;

                case MIN_LENGTH:
                    if (value != null && value instanceof String) {
                        int length = ((String) value).length();
                        int min = (Integer) rule.param;
                        if (length < min) {
                            errors.add(new FieldError(field, "Minimum length is " + min));
                        }
                    }
                    break;

                case MAX_LENGTH:
                    if (value != null && value instanceof String) {
                        int length = ((String) value).length();
                        int max = (Integer) rule.param;
                        if (length > max) {
                            errors.add(new FieldError(field, "Maximum length is " + max));
                        }
                    }
                    break;

                case PATTERN:
                    if (value != null && !value.toString().isEmpty()) {
                        Pattern pattern = (Pattern) rule.param;
                        if (!pattern.matcher(value.toString()).matches()) {
                            errors.add(new FieldError(field, "Invalid format"));
                        }
                    }
                    break;

                case MIN:
                    if (value instanceof Number) {
                        Number num = (Number) value;
                        Number min = (Number) rule.param;
                        if (num.doubleValue() < min.doubleValue()) {
                            errors.add(new FieldError(field, "Minimum value is " + min));
                        }
                    }
                    break;

                case MAX:
                    if (value instanceof Number) {
                        Number num = (Number) value;
                        Number max = (Number) rule.param;
                        if (num.doubleValue() > max.doubleValue()) {
                            errors.add(new FieldError(field, "Maximum value is " + max));
                        }
                    }
                    break;

                case TYPE:
                    if (value != null) {
                        Class<?> expectedType = (Class<?>) rule.param;
                        if (!expectedType.isInstance(value)) {
                            errors.add(new FieldError(field, "Expected type " + expectedType.getSimpleName()));
                        }
                    }
                    break;

                case CUSTOM:
                    if (value != null) {
                        CustomValidation custom = (CustomValidation) rule.param;
                        if (!custom.predicate.test(value)) {
                            errors.add(new FieldError(field, custom.errorMessage));
                        }
                    }
                    break;
            }
        }

        return new ValidationResult(errors);
    }

    private enum RuleType {
        REQUIRED,
        EMAIL,
        UUID,
        MIN_LENGTH,
        MAX_LENGTH,
        PATTERN,
        MIN,
        MAX,
        TYPE,
        CUSTOM
    }

    private static class ValidationRule {
        final String field;
        final RuleType type;
        final Object param;

        ValidationRule(String field, RuleType type, Object param) {
            this.field = field;
            this.type = type;
            this.param = param;
        }
    }

    @FunctionalInterface
    public interface ValidationPredicate {
        boolean test(Object value);
    }

    private static class CustomValidation {
        final ValidationPredicate predicate;
        final String errorMessage;

        CustomValidation(ValidationPredicate predicate, String errorMessage) {
            this.predicate = predicate;
            this.errorMessage = errorMessage;
        }
    }
}
