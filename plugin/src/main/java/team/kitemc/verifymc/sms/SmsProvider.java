package team.kitemc.verifymc.sms;

public interface SmsProvider {
    SmsSendResult sendVerificationCode(String phone, String code, int expireMinutes);
}
