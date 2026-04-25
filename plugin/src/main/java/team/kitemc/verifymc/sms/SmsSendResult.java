package team.kitemc.verifymc.sms;

public record SmsSendResult(boolean success, String message) {
    public static SmsSendResult ok() {
        return new SmsSendResult(true, "");
    }

    public static SmsSendResult failure(String message) {
        return new SmsSendResult(false, message == null ? "" : message);
    }
}
