package team.kitemc.verifymc.web;

import java.util.function.BiFunction;
import org.json.JSONObject;
import team.kitemc.verifymc.util.EmailAddressUtil;
import team.kitemc.verifymc.web.handler.AuthFlowSupport;

public record RegistrationRequest(
        String email,
        String code,
        String phone,
        String smsCode,
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
        String email = EmailAddressUtil.normalize(req.optString("email", ""));
        String code = req.optString("code");
        String rawPhone = req.optString("phone", "");
        String countryCode = req.optString("countryCode", "");
        String phone = AuthFlowSupport.missingRequiredCountryCode(rawPhone, countryCode)
                ? ""
                : AuthFlowSupport.normalizePhone(countryCode, rawPhone);
        String smsCode = req.optString("smsCode", req.optString("phoneCode", ""));
        String username = req.optString("username");
        String password = req.optString("password", "");
        String captchaToken = req.optString("captchaToken", "");
        String captchaAnswer = req.optString("captchaAnswer", "");
        String language = req.optString("language", "en");
        String platform = req.optString("platform", "java");
        JSONObject questionnaire = req.optJSONObject("questionnaire");
        String normalizedUsername = usernameNormalizer.apply(username, platform);
        return new RegistrationRequest(email, code, phone, smsCode, username, normalizedUsername, password,
                captchaToken, captchaAnswer, language, platform, questionnaire);
    }
}
