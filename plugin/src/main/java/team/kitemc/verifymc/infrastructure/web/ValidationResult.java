package team.kitemc.verifymc.infrastructure.web;

import team.kitemc.verifymc.infrastructure.exception.ValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {
    private final List<FieldError> errors;

    public ValidationResult(List<FieldError> errors) {
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<FieldError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public void throwIfInvalid() {
        if (!isValid()) {
            throw new ValidationException(convertErrors());
        }
    }

    private List<ValidationException.FieldError> convertErrors() {
        List<ValidationException.FieldError> result = new ArrayList<>();
        for (FieldError error : errors) {
            result.add(new ValidationException.FieldError(error.getFieldName(), error.getErrorMessage()));
        }
        return result;
    }

    public String getFirstError() {
        if (errors.isEmpty()) {
            return null;
        }
        return errors.get(0).getErrorMessage();
    }

    public String getErrorForField(String fieldName) {
        for (FieldError error : errors) {
            if (error.getFieldName().equals(fieldName)) {
                return error.getErrorMessage();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        if (isValid()) {
            return "ValidationResult[valid=true]";
        }
        return "ValidationResult[valid=false, errors=" + errors + "]";
    }
}
