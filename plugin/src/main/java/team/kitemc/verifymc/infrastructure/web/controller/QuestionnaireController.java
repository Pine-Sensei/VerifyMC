package team.kitemc.verifymc.infrastructure.web.controller;

import org.bukkit.plugin.Plugin;
import org.json.JSONArray;
import org.json.JSONObject;
import team.kitemc.verifymc.domain.service.QuestionnaireEvaluationService;
import team.kitemc.verifymc.infrastructure.annotation.Service;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;
import team.kitemc.verifymc.infrastructure.web.ApiResponse;
import team.kitemc.verifymc.infrastructure.web.RequestContext;
import team.kitemc.verifymc.infrastructure.web.RouteHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuestionnaireController implements RouteHandler {

    private final Plugin plugin;
    private final QuestionnaireEvaluationService questionnaireEvaluationService;
    private final ConfigurationService configService;
    private final boolean debug;

    private final ConcurrentHashMap<String, QuestionnaireSubmissionRecord> questionnaireSubmissionStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WindowRateLimitRecord> questionnaireRateLimitStore = new ConcurrentHashMap<>();

    public QuestionnaireController(Plugin plugin,
                                    QuestionnaireEvaluationService questionnaireEvaluationService,
                                    ConfigurationService configService) {
        this.plugin = plugin;
        this.questionnaireEvaluationService = questionnaireEvaluationService;
        this.configService = configService;
        this.debug = configService.getBoolean("debug", false);
    }

    @Override
    public void handle(RequestContext ctx) throws Exception {
        String path = ctx.getPath();

        if ("/api/questionnaire".equals(path)) {
            handleGetQuestionnaire(ctx);
        } else if ("/api/submit-questionnaire".equals(path)) {
            handleSubmitQuestionnaire(ctx);
        } else {
            ctx.sendNotFound("Endpoint not found");
        }
    }

    private void handleGetQuestionnaire(RequestContext ctx) throws IOException {
        if (!"GET".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        String language = ctx.getLanguage();

        JSONObject resp = new JSONObject();
        try {
            JSONObject questionnaire = questionnaireEvaluationService.getQuestionnaire(language);
            resp.put("success", true);
            resp.put("data", questionnaire);
        } catch (Exception e) {
            debugLog("Failed to get questionnaire: " + e.getMessage());
            resp.put("success", false);
            resp.put("msg", "Failed to get questionnaire");
        }

        ctx.sendJson(resp);
    }

    private void handleSubmitQuestionnaire(RequestContext ctx) throws IOException {
        if (!"POST".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        JSONObject body = ctx.getBody();
        String language = body.optString("language", "en");
        String requestId = UUID.randomUUID().toString();
        String clientIp = ctx.getClientIp();
        String requestUuid = body.optString("uuid", "").trim().toLowerCase();
        String requestEmail = body.optString("email", "").trim().toLowerCase();

        int ipLimit = configService.getInt("questionnaire.rate_limit.ip.max", 20);
        int uuidLimit = configService.getInt("questionnaire.rate_limit.uuid.max", 8);
        int emailLimit = configService.getInt("questionnaire.rate_limit.email.max", 6);
        long windowMs = configService.getLong("questionnaire.rate_limit.window_ms", 300000L);

        RateLimitDecision ipDecision = checkQuestionnaireRateLimit("q:ip:" + clientIp, ipLimit, windowMs);
        RateLimitDecision uuidDecision = checkQuestionnaireRateLimit("q:uuid:" + requestUuid, uuidLimit, windowMs);
        RateLimitDecision emailDecision = checkQuestionnaireRateLimit("q:email:" + requestEmail, emailLimit, windowMs);

        if (!ipDecision.allowed || !uuidDecision.allowed || !emailDecision.allowed) {
            long retryAfterMs = Math.max(ipDecision.retryAfterMs, 
                Math.max(uuidDecision.retryAfterMs, emailDecision.retryAfterMs));
            JSONObject resp = new JSONObject();
            resp.put("success", false);
            resp.put("msg", "Too many questionnaire submissions, please retry later.");
            resp.put("retry_after_ms", retryAfterMs);
            ctx.sendJson(resp);
            return;
        }

        JSONObject resp = new JSONObject();

        try {
            JSONObject answersJson = body.optJSONObject("answers");
            if (answersJson == null) {
                resp.put("success", false);
                resp.put("msg", "questionnaire.answers_required");
                ctx.sendJson(resp);
                return;
            }

            JSONObject questionnaire = questionnaireEvaluationService.getQuestionnaire(language);
            JSONArray questionDefs = questionnaire.optJSONArray("questions");
            if (questionDefs == null) {
                resp.put("success", false);
                resp.put("msg", "questionnaire.answers_required");
                ctx.sendJson(resp);
                return;
            }

            Map<Integer, JSONObject> questionDefMap = new HashMap<>();
            for (int i = 0; i < questionDefs.length(); i++) {
                JSONObject questionDef = questionDefs.optJSONObject(i);
                if (questionDef != null) {
                    questionDefMap.put(questionDef.optInt("id", -1), questionDef);
                }
            }

            Map<Integer, QuestionnaireEvaluationService.QuestionAnswer> answers = new HashMap<>();
            for (String key : answersJson.keySet()) {
                int questionId = Integer.parseInt(key);
                JSONObject questionDef = questionDefMap.get(questionId);
                if (questionDef == null) {
                    throw new IllegalArgumentException("Invalid question id: " + questionId);
                }

                Object rawAnswer = answersJson.get(key);
                if (!(rawAnswer instanceof JSONObject)) {
                    throw new IllegalArgumentException("Invalid answer object for question: " + questionId);
                }
                JSONObject answerObj = (JSONObject) rawAnswer;

                String answerType = answerObj.optString("type", "").trim();
                String questionType = questionDef.optString("type", "single_choice");
                if (answerType.isEmpty()) {
                    answerType = questionType;
                }
                if (!answerType.equals(questionType)) {
                    throw new IllegalArgumentException("Illegal answer type for question: " + questionId);
                }
                if (!isSupportedQuestionType(answerType)) {
                    throw new IllegalArgumentException("Unsupported question type: " + answerType);
                }

                JSONArray selectedArray = answerObj.optJSONArray("selectedOptionIds");
                List<Integer> selectedOptionIds = new ArrayList<>();
                if (selectedArray != null) {
                    for (int i = 0; i < selectedArray.length(); i++) {
                        selectedOptionIds.add(selectedArray.getInt(i));
                    }
                }

                String textAnswer = answerObj.optString("textAnswer", "");
                validateAnswer(questionDef, answerType, selectedOptionIds, textAnswer, questionId);
                answers.put(questionId, new QuestionnaireEvaluationService.QuestionAnswer(
                    answerType, selectedOptionIds, textAnswer));
            }

            QuestionnaireEvaluationService.QuestionnaireResult result = 
                questionnaireEvaluationService.evaluateAnswers(answers);
            JSONObject resultJson = result.toJson();
            JSONArray details = resultJson.optJSONArray("details");
            boolean manualReviewRequired = resultJson.optBoolean("manual_review_required", false);

            long submittedAt = System.currentTimeMillis();
            long expiresAt = submittedAt + 10 * 60 * 1000;
            String questionnaireToken = UUID.randomUUID().toString();
            questionnaireSubmissionStore.put(questionnaireToken, new QuestionnaireSubmissionRecord(
                result.isPassed(),
                result.getScore(),
                result.getPassScore(),
                details,
                manualReviewRequired,
                submittedAt
            ));

            resp.put("success", true);
            resp.put("passed", result.isPassed());
            resp.put("score", result.getScore());
            resp.put("pass_score", result.getPassScore());
            resp.put("details", details);
            resp.put("manual_review_required", manualReviewRequired);
            resp.put("token", questionnaireToken);
            resp.put("submitted_at", submittedAt);
            resp.put("expires_at", expiresAt);
            resp.put("msg", result.isPassed() ? 
                "questionnaire.passed" : "questionnaire.failed");

        } catch (IllegalArgumentException e) {
            debugLog("Questionnaire validation error: " + e.getMessage());
            resp.put("success", false);
            resp.put("msg", e.getMessage());
        } catch (Exception e) {
            debugLog("Failed to submit questionnaire: " + e.getMessage());
            resp.put("success", false);
            resp.put("msg", "Failed to submit questionnaire: " + e.getMessage());
        }

        ctx.sendJson(resp);
    }

    private boolean isSupportedQuestionType(String type) {
        return "single_choice".equals(type) || "multiple_choice".equals(type) || "text".equals(type);
    }

    private void validateAnswer(JSONObject questionDef, String answerType, 
                                List<Integer> selectedOptionIds, String textAnswer, int questionId) {
        boolean required = questionDef.optBoolean("required", false);
        JSONObject input = questionDef.optJSONObject("input");
        int minSelections = input != null ? input.optInt("min_selections", 0) : 0;
        int maxSelections = input != null ? input.optInt("max_selections", Integer.MAX_VALUE) : Integer.MAX_VALUE;
        int minLength = input != null ? input.optInt("min_length", 0) : 0;
        int maxLength = input != null ? input.optInt("max_length", Integer.MAX_VALUE) : Integer.MAX_VALUE;

        if ("single_choice".equals(answerType) || "multiple_choice".equals(answerType)) {
            JSONArray options = questionDef.optJSONArray("options");
            int optionCount = options != null ? options.length() : 0;
            if (required && selectedOptionIds.isEmpty()) {
                throw new IllegalArgumentException("Question " + questionId + " is required");
            }
            if (selectedOptionIds.size() < minSelections || selectedOptionIds.size() > maxSelections) {
                throw new IllegalArgumentException("Invalid selection count for question: " + questionId);
            }
            for (Integer optionId : selectedOptionIds) {
                if (optionId == null || optionId < 0 || optionId >= optionCount) {
                    throw new IllegalArgumentException("Invalid option id for question: " + questionId);
                }
            }
        } else if ("text".equals(answerType)) {
            String normalized = textAnswer != null ? textAnswer.trim() : "";
            if (required && normalized.isEmpty()) {
                throw new IllegalArgumentException("Question " + questionId + " is required");
            }
            if (!normalized.isEmpty() && (normalized.length() < minLength || normalized.length() > maxLength)) {
                throw new IllegalArgumentException("Invalid text length for question: " + questionId);
            }
        }
    }

    private RateLimitDecision checkQuestionnaireRateLimit(String key, int limit, long windowMs) {
        if (key == null || key.isBlank() || limit <= 0 || windowMs <= 0) {
            return new RateLimitDecision(true, 0L);
        }
        long now = System.currentTimeMillis();
        WindowRateLimitRecord rec = questionnaireRateLimitStore.compute(key, (k, old) -> {
            if (old == null || now - old.windowStart >= windowMs) {
                return new WindowRateLimitRecord(1, now);
            }
            old.count++;
            return old;
        });
        if (rec.count > limit) {
            long retryAfterMs = windowMs - (now - rec.windowStart);
            return new RateLimitDecision(false, Math.max(1L, retryAfterMs));
        }
        return new RateLimitDecision(true, 0L);
    }

    public QuestionnaireSubmissionRecord getSubmissionRecord(String token) {
        return questionnaireSubmissionStore.get(token);
    }

    public void removeSubmissionRecord(String token) {
        questionnaireSubmissionStore.remove(token);
    }

    private void debugLog(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] QuestionnaireController: " + msg);
        }
    }

    private static class WindowRateLimitRecord {
        int count;
        long windowStart;

        WindowRateLimitRecord(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }

    private static class RateLimitDecision {
        final boolean allowed;
        final long retryAfterMs;

        RateLimitDecision(boolean allowed, long retryAfterMs) {
            this.allowed = allowed;
            this.retryAfterMs = retryAfterMs;
        }
    }

    public static class QuestionnaireSubmissionRecord {
        private final boolean passed;
        private final int score;
        private final int passScore;
        private final JSONArray details;
        private final boolean manualReviewRequired;
        private final long submittedAt;

        public QuestionnaireSubmissionRecord(boolean passed, int score, int passScore, 
                                             JSONArray details, boolean manualReviewRequired, long submittedAt) {
            this.passed = passed;
            this.score = score;
            this.passScore = passScore;
            this.details = details;
            this.manualReviewRequired = manualReviewRequired;
            this.submittedAt = submittedAt;
        }

        public boolean isPassed() { return passed; }
        public int getScore() { return score; }
        public int getPassScore() { return passScore; }
        public JSONArray getDetails() { return details; }
        public boolean isManualReviewRequired() { return manualReviewRequired; }
        public long getSubmittedAt() { return submittedAt; }
    }
}
