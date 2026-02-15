package team.kitemc.verifymc.service;

import team.kitemc.verifymc.service.CaptchaService.CaptchaResult;

public interface ICaptchaService {
    CaptchaResult generateCaptcha();
    boolean validateCaptcha(String token, String answer);
    boolean isCaptchaEnabled();
}
