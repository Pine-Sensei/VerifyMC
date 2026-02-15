package team.kitemc.verifymc.infrastructure.web;

public class FieldError {
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

    @Override
    public String toString() {
        return fieldName + ": " + errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldError fieldError = (FieldError) o;
        return fieldName.equals(fieldError.fieldName) && errorMessage.equals(fieldError.errorMessage);
    }

    @Override
    public int hashCode() {
        return 31 * fieldName.hashCode() + errorMessage.hashCode();
    }
}
