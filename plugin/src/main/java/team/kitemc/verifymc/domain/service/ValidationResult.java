package team.kitemc.verifymc.domain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : Collections.emptyList();
        this.warnings = warnings != null ? Collections.unmodifiableList(new ArrayList<>(warnings)) : Collections.emptyList();
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    public String getFirstWarning() {
        return warnings.isEmpty() ? null : warnings.get(0);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, errors, Collections.emptyList());
    }

    public static ValidationResult invalid(String error) {
        return new ValidationResult(false, Collections.singletonList(error), Collections.emptyList());
    }

    public static ValidationResult withWarnings(List<String> warnings) {
        return new ValidationResult(true, Collections.emptyList(), warnings);
    }

    public static ValidationResult withWarnings(String warning) {
        return new ValidationResult(true, Collections.emptyList(), Collections.singletonList(warning));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public Builder addError(String error) {
            errors.add(error);
            return this;
        }

        public Builder addWarning(String warning) {
            warnings.add(warning);
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(errors.isEmpty(), errors, warnings);
        }
    }
}
