package team.kitemc.verifymc.sms;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;
import team.kitemc.verifymc.core.ConfigManager;

public class TencentSmsProvider implements SmsProvider {
    private static final String SERVICE = "sms";
    private static final String VERSION = "2021-01-11";
    private static final String ACTION = "SendSms";

    private final ConfigManager config;
    private final HttpClient httpClient;

    public TencentSmsProvider(ConfigManager config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getSmsConnectTimeoutMs()))
                .build();
    }

    @Override
    public SmsSendResult sendVerificationCode(String phone, String code, int expireMinutes) {
        if (config.getTencentSmsSecretId().isEmpty() || config.getTencentSmsSecretKey().isEmpty()
                || config.getTencentSmsSdkAppId().isEmpty() || config.getTencentSmsTemplateId().isEmpty()) {
            return SmsSendResult.failure("missing tencent sms config");
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("SmsSdkAppId", config.getTencentSmsSdkAppId());
            payload.put("SignName", config.getTencentSmsSignName());
            payload.put("TemplateId", config.getTencentSmsTemplateId());
            payload.put("TemplateParamSet", new JSONArray().put(code).put(String.valueOf(expireMinutes)));
            payload.put("PhoneNumberSet", new JSONArray().put(phone));
            String body = payload.toString();

            long timestamp = Instant.now().getEpochSecond();
            String date = Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String authorization = buildAuthorization(body, timestamp, date);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + config.getTencentSmsEndpoint()))
                    .timeout(Duration.ofMillis(config.getSmsRequestTimeoutMs()))
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Host", config.getTencentSmsEndpoint())
                    .header("X-TC-Action", ACTION)
                    .header("X-TC-Timestamp", String.valueOf(timestamp))
                    .header("X-TC-Version", VERSION)
                    .header("X-TC-Region", config.getTencentSmsRegion())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body()).optJSONObject("Response");
            if (response.statusCode() >= 200 && response.statusCode() < 300 && json != null) {
                JSONArray statuses = json.optJSONArray("SendStatusSet");
                if (statuses != null && !statuses.isEmpty()
                        && "Success".equalsIgnoreCase(statuses.optJSONObject(0).optString("Code"))) {
                    return SmsSendResult.ok();
                }
                if (json.has("Error")) {
                    return SmsSendResult.failure(json.getJSONObject("Error").optString("Message", "tencent sms send failed"));
                }
            }
            return SmsSendResult.failure("tencent sms send failed");
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            return SmsSendResult.failure(e.getMessage());
        } catch (Exception e) {
            return SmsSendResult.failure(e.getMessage());
        }
    }

    private String buildAuthorization(String body, long timestamp, String date) throws Exception {
        String canonicalRequest = "POST\n/\n\ncontent-type:application/json; charset=utf-8\nhost:" + config.getTencentSmsEndpoint()
                + "\n\ncontent-type;host\n" + sha256Hex(body);
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
        byte[] secretDate = hmacSha256(("TC3" + config.getTencentSmsSecretKey()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        String signature = bytesToHex(hmacSha256(secretSigning, stringToSign));
        return "TC3-HMAC-SHA256 Credential=" + config.getTencentSmsSecretId() + "/" + credentialScope
                + ", SignedHeaders=content-type;host, Signature=" + signature;
    }

    private byte[] hmacSha256(byte[] key, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return bytesToHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
