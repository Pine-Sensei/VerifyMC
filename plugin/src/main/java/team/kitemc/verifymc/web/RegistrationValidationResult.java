package team.kitemc.verifymc.web;

import org.json.JSONObject;

/**
 * Validation result for registration requests.
 * (Preserved from original.)
 */
public record RegistrationValidationResult(boolean passed, String messageKey, JSONObject responseFields) {

    public static RegistrationValidationResult pass() {
        return new RegistrationValidationResult(true, null, new JSONObject());
    }

    public static RegistrationValidationResult reject(String messageKey) {
        return reject(messageKey, new JSONObject());
    }

    public static RegistrationValidationResult reject(String messageKey, JSONObject responseFields) {
        return new RegistrationValidationResult(false, messageKey, responseFields == null ? new JSONObject() : responseFields);
    }
}
