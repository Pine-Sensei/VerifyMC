package team.kitemc.verifymc.sms;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONObject;
import team.kitemc.verifymc.core.ConfigManager;
import team.kitemc.verifymc.util.PhoneNumberUtil;

public class AliyunSmsProvider implements SmsProvider {
    private static final String ALGORITHM = "ACS3-HMAC-SHA256";
    private static final String ACTION = "SendSms";
    private static final String VERSION = "2017-05-25";
    private static final String EMPTY_BODY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final DateTimeFormatter ACS_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    private final ConfigManager config;
    private final HttpClient httpClient;

    public AliyunSmsProvider(ConfigManager config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getSmsConnectTimeoutMs()))
                .build();
    }

    @Override
    public SmsSendResult sendVerificationCode(String phone, String code, int expireMinutes) {
        if (config.getAliyunSmsAccessKeyId().isEmpty() || config.getAliyunSmsAccessKeySecret().isEmpty()
                || config.getAliyunSmsSignName().isEmpty() || config.getAliyunSmsTemplateCode().isEmpty()) {
            return SmsSendResult.failure("missing aliyun sms config");
        }

        try {
            Map<String, String> params = new TreeMap<>();
            params.put("PhoneNumbers", PhoneNumberUtil.toAliyunPhoneNumber(phone));
            params.put("SignName", config.getAliyunSmsSignName());
            params.put("TemplateCode", config.getAliyunSmsTemplateCode());
            params.put("TemplateParam", buildTemplateParam(code, expireMinutes));

            String canonicalQuery = canonicalize(params);
            String acsDate = ACS_DATE_FORMATTER.format(Instant.now());
            String nonce = UUID.randomUUID().toString();
            AuthorizationData authorizationData = buildAuthorization(
                    config.getAliyunSmsAccessKeyId(),
                    config.getAliyunSmsAccessKeySecret(),
                    config.getAliyunSmsEndpoint(),
                    ACTION,
                    VERSION,
                    acsDate,
                    nonce,
                    canonicalQuery,
                    EMPTY_BODY_HASH);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + config.getAliyunSmsEndpoint() + "/?" + canonicalQuery))
                    .timeout(Duration.ofMillis(config.getSmsRequestTimeoutMs()))
                    .header("Accept", "application/json")
                    .header("Authorization", authorizationData.authorization())
                    .header("x-acs-action", ACTION)
                    .header("x-acs-version", VERSION)
                    .header("x-acs-date", acsDate)
                    .header("x-acs-signature-nonce", nonce)
                    .header("x-acs-content-sha256", EMPTY_BODY_HASH)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300 && "OK".equalsIgnoreCase(json.optString("Code"))) {
                return SmsSendResult.ok();
            }
            return SmsSendResult.failure(json.optString("Message", "aliyun sms send failed"));
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            return SmsSendResult.failure(e.getMessage());
        } catch (Exception e) {
            return SmsSendResult.failure(e.getMessage());
        }
    }

    private String buildTemplateParam(String code, int expireMinutes) {
        JSONObject params = new JSONObject();
        params.put(config.getAliyunSmsCodeParamName(), code);
        params.put(config.getAliyunSmsExpireParamName(), String.valueOf(expireMinutes));
        return params.toString();
    }

    private String canonicalize(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> percentEncode(entry.getKey()) + "=" + percentEncode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    static AuthorizationData buildAuthorization(
            String accessKeyId,
            String accessKeySecret,
            String host,
            String action,
            String version,
            String acsDate,
            String nonce,
            String canonicalQuery,
            String hashedBody
    ) throws Exception {
        String signedHeaders = "host;x-acs-action;x-acs-content-sha256;x-acs-date;x-acs-signature-nonce;x-acs-version";
        String canonicalHeaders = "host:" + host + "\n"
                + "x-acs-action:" + action + "\n"
                + "x-acs-content-sha256:" + hashedBody + "\n"
                + "x-acs-date:" + acsDate + "\n"
                + "x-acs-signature-nonce:" + nonce + "\n"
                + "x-acs-version:" + version + "\n";
        String canonicalRequest = "POST\n/\n" + canonicalQuery + "\n"
                + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedBody;
        String stringToSign = ALGORITHM + "\n" + sha256Hex(canonicalRequest);
        String signature = hmacSha256Hex(accessKeySecret, stringToSign);
        String authorization = ALGORITHM + " Credential=" + accessKeyId
                + ",SignedHeaders=" + signedHeaders
                + ",Signature=" + signature;
        return new AuthorizationData(authorization, canonicalRequest, stringToSign, signature, signedHeaders);
    }

    static String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    static String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return bytesToHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String hmacSha256Hex(String key, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return bytesToHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    record AuthorizationData(
            String authorization,
            String canonicalRequest,
            String stringToSign,
            String signature,
            String signedHeaders
    ) {}
}
