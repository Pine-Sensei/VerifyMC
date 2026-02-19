package team.kitemc.verifymc.web;

import org.json.JSONObject;
import java.util.function.BiFunction;

/**
 * Registration request data transfer object.
 * (Preserved from original.)
 */
public record RegistrationRequest(
        String email,
        String code,
        String uuid,
        String username,
        String normalizedUsername,
        String password,
        String captchaToken,
        String captchaAnswer,
        String language,
        String platform,
        JSONObject questionnaire
) {
    public static RegistrationRequest fromJson(JSONObject req, BiFunction<String, String, String> usernameNormalizer) {
        String email = req.optString("email", "").trim().toLowerCase();
        String code = req.optString("code");
        String uuid = req.optString("uuid");
        String username = req.optString("username");
        String password = req.optString("password", "");
        String captchaToken = req.optString("captchaToken", "");
        String captchaAnswer = req.optString("captchaAnswer", "");
        String language = req.optString("language", "en");
        String platform = req.optString("platform", "java");
        JSONObject questionnaire = req.optJSONObject("questionnaire");
        String normalizedUsername = usernameNormalizer.apply(username, platform);
        return new RegistrationRequest(email, code, uuid, username, normalizedUsername, password, captchaToken, captchaAnswer, language, platform, questionnaire);
    }
}
