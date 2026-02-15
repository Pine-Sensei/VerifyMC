package team.kitemc.verifymc.domain.service;

import org.json.JSONArray;
import org.json.JSONObject;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;
import team.kitemc.verifymc.infrastructure.persistence.QuestionnaireRepository;
import team.kitemc.verifymc.infrastructure.persistence.QuestionnaireRepository.QuestionDefinition;
import team.kitemc.verifymc.infrastructure.persistence.QuestionnaireRepository.QuestionOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class QuestionnaireEvaluationService {

    private final ConfigurationService configService;
    private final QuestionnaireRepository questionnaireRepository;
    private final LLMScoringService llmScoringService;
    private final boolean debug;

    public QuestionnaireEvaluationService(ConfigurationService configService,
                                          QuestionnaireRepository questionnaireRepository,
                                          LLMScoringService llmScoringService) {
        this.configService = configService;
        this.questionnaireRepository = questionnaireRepository;
        this.llmScoringService = llmScoringService;
        this.debug = configService.getBoolean("debug", false);
    }

    public boolean isEnabled() {
        return configService.getBoolean("questionnaire.enabled", false);
    }

    public int getPassScore() {
        return configService.getInt("questionnaire.pass_score", 60);
    }

    public boolean hasTextQuestions() {
        for (QuestionDefinition question : questionnaireRepository.getQuestions()) {
            if (question.isTextType()) {
                return true;
            }
        }
        return false;
    }

    public JSONObject getQuestionnaire(String language) {
        JSONObject result = new JSONObject();
        result.put("enabled", isEnabled());
        result.put("pass_score", getPassScore());

        if (!isEnabled()) {
            result.put("questions", new JSONArray());
            return result;
        }

        JSONArray questionsArray = new JSONArray();
        for (QuestionDefinition question : questionnaireRepository.getQuestions()) {
            JSONObject questionJson = buildQuestionJson(question, language);
            questionsArray.put(questionJson);
        }

        result.put("questions", questionsArray);
        return result;
    }

    @SuppressWarnings("unchecked")
    private JSONObject buildQuestionJson(QuestionDefinition question, String language) {
        JSONObject questionJson = new JSONObject();
        questionJson.put("id", question.getId());

        String questionText = "zh".equals(language) ? question.getQuestionZh() : question.getQuestionEn();
        questionJson.put("question", questionText);
        questionJson.put("type", question.getType());
        questionJson.put("required", question.isRequired());

        JSONObject inputMeta = new JSONObject();
        if (question.getInput() != null) {
            for (Map.Entry<String, Object> entry : question.getInput().entrySet()) {
                inputMeta.put(entry.getKey(), entry.getValue());
            }
        }

        if (question.isTextType() && question.getInput() != null) {
            String placeholder = "zh".equals(language)
                    ? String.valueOf(question.getInput().getOrDefault("placeholder_zh", ""))
                    : String.valueOf(question.getInput().getOrDefault("placeholder_en", ""));
            inputMeta.put("placeholder", placeholder);
        }

        questionJson.put("input", inputMeta);

        JSONArray optionsArray = new JSONArray();
        List<QuestionOption> options = question.getOptions();
        for (int i = 0; i < options.size(); i++) {
            QuestionOption opt = options.get(i);
            JSONObject optionJson = new JSONObject();
            optionJson.put("id", i);
            String optText = "zh".equals(language) ? opt.getTextZh() : opt.getTextEn();
            optionJson.put("text", optText);
            optionsArray.put(optionJson);
        }
        questionJson.put("options", optionsArray);

        return questionJson;
    }

    public QuestionnaireResult evaluateAnswers(Map<Integer, QuestionAnswer> answers) {
        if (!isEnabled()) {
            return new QuestionnaireResult(true, 100, getPassScore(), Collections.emptyList());
        }

        int totalScore = 0;
        List<QuestionScoreDetail> details = new ArrayList<>();

        for (QuestionDefinition question : questionnaireRepository.getQuestions()) {
            QuestionAnswer answer = answers.get(question.getId());

            if (answer == null) {
                details.add(new QuestionScoreDetail(
                        question.getId(),
                        question.getType(),
                        0,
                        question.calculateMaxScore(),
                        "No answer submitted",
                        0.0D,
                        false,
                        "local",
                        "",
                        "",
                        0L,
                        0
                ));
                continue;
            }

            QuestionScoreDetail detail;
            if (question.isTextType()) {
                detail = scoreTextQuestion(question, answer);
            } else {
                detail = scoreChoiceQuestion(question, answer);
            }

            totalScore += detail.getScore();
            details.add(detail);
        }

        int passScore = getPassScore();
        boolean passed = totalScore >= passScore;
        debugLog("Questionnaire evaluation: score=" + totalScore + ", passScore=" + passScore + ", passed=" + passed);

        return new QuestionnaireResult(passed, totalScore, passScore, details);
    }

    private QuestionScoreDetail scoreChoiceQuestion(QuestionDefinition question, QuestionAnswer answer) {
        int questionScore = 0;
        int maxScore = question.calculateMaxScore();

        List<QuestionOption> options = question.getOptions();
        for (int optionId : answer.getSelectedOptionIds()) {
            if (optionId >= 0 && optionId < options.size()) {
                questionScore += options.get(optionId).getScore();
            }
        }

        questionScore = Math.max(0, Math.min(maxScore, questionScore));
        return new QuestionScoreDetail(
                question.getId(),
                answer.getType(),
                questionScore,
                maxScore,
                "Locally scored",
                1.0D,
                false,
                "local",
                "",
                "",
                0L,
                0
        );
    }

    private QuestionScoreDetail scoreTextQuestion(QuestionDefinition question, QuestionAnswer answer) {
        int maxScore = question.calculateMaxScore();

        if (llmScoringService == null || !llmScoringService.isAvailable()) {
            return new QuestionScoreDetail(
                    question.getId(),
                    answer.getType(),
                    0,
                    maxScore,
                    "LLM scoring disabled by config, requires manual review",
                    0.0D,
                    true,
                    "manual",
                    "",
                    "",
                    0L,
                    0
            );
        }

        String questionText = resolveQuestionText(question);
        String scoringRule = resolveScoringRule(question);

        LLMScoringService.EssayScoringRequest request = new LLMScoringService.EssayScoringRequest(
                question.getId(),
                questionText,
                answer.getTextAnswer(),
                scoringRule,
                maxScore
        );

        LLMScoringService.EssayScoringResult result = llmScoringService.score(request);

        return new QuestionScoreDetail(
                question.getId(),
                answer.getType(),
                result.getScore(),
                maxScore,
                result.getReason(),
                result.getConfidence(),
                result.isManualReview(),
                result.getProvider(),
                result.getModel(),
                result.getRequestId(),
                result.getLatencyMs(),
                result.getRetryCount()
        );
    }

    private String resolveQuestionText(QuestionDefinition question) {
        String zh = question.getQuestionZh().trim();
        String en = question.getQuestionEn().trim();
        if (!zh.isEmpty() && !en.isEmpty()) {
            return "[ZH] " + zh + "\n[EN] " + en;
        }
        return !zh.isEmpty() ? zh : en;
    }

    private String resolveScoringRule(QuestionDefinition question) {
        String localRule = question.getScoringRule();
        if (localRule != null && !localRule.trim().isEmpty()) {
            return localRule.trim();
        }
        return configService.getString("llm.scoring_rule", "Evaluate relevance, detail and rule-awareness.");
    }

    public void reload() {
        questionnaireRepository.load();
    }

    private void debugLog(String msg) {
        if (debug) {
            System.out.println("[DEBUG] QuestionnaireEvaluationService: " + msg);
        }
    }

    public static class QuestionAnswer {
        private final String type;
        private final List<Integer> selectedOptionIds;
        private final String textAnswer;

        public QuestionAnswer(String type, List<Integer> selectedOptionIds, String textAnswer) {
            this.type = type != null ? type : "";
            this.selectedOptionIds = selectedOptionIds != null ? new ArrayList<>(selectedOptionIds) : new ArrayList<>();
            this.textAnswer = textAnswer != null ? textAnswer : "";
        }

        public String getType() { return type; }
        public List<Integer> getSelectedOptionIds() { return Collections.unmodifiableList(selectedOptionIds); }
        public String getTextAnswer() { return textAnswer; }
    }

    public static class QuestionScoreDetail {
        private final int questionId;
        private final String type;
        private final int score;
        private final int maxScore;
        private final String reason;
        private final double confidence;
        private final boolean manualReview;
        private final String provider;
        private final String model;
        private final String requestId;
        private final long latencyMs;
        private final int retryCount;

        public QuestionScoreDetail(int questionId, String type, int score, int maxScore,
                                   String reason, double confidence, boolean manualReview,
                                   String provider, String model, String requestId,
                                   long latencyMs, int retryCount) {
            this.questionId = questionId;
            this.type = type != null ? type : "";
            this.score = score;
            this.maxScore = maxScore;
            this.reason = reason != null ? reason : "";
            this.confidence = Math.max(0.0D, Math.min(1.0D, confidence));
            this.manualReview = manualReview;
            this.provider = provider != null ? provider : "";
            this.model = model != null ? model : "";
            this.requestId = requestId != null ? requestId : "";
            this.latencyMs = Math.max(0L, latencyMs);
            this.retryCount = Math.max(0, retryCount);
        }

        public int getQuestionId() { return questionId; }
        public String getType() { return type; }
        public int getScore() { return score; }
        public int getMaxScore() { return maxScore; }
        public String getReason() { return reason; }
        public double getConfidence() { return confidence; }
        public boolean isManualReview() { return manualReview; }
        public String getProvider() { return provider; }
        public String getModel() { return model; }
        public String getRequestId() { return requestId; }
        public long getLatencyMs() { return latencyMs; }
        public int getRetryCount() { return retryCount; }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("question_id", questionId);
            json.put("type", type);
            json.put("score", score);
            json.put("max_score", maxScore);
            json.put("reason", reason);
            json.put("confidence", confidence);
            json.put("manual_review", manualReview);
            json.put("provider", provider);
            json.put("model", model);
            json.put("request_id", requestId);
            json.put("latency_ms", latencyMs);
            json.put("retry_count", retryCount);
            return json;
        }
    }

    public static class QuestionnaireResult {
        private final boolean passed;
        private final int score;
        private final int passScore;
        private final List<QuestionScoreDetail> details;

        public QuestionnaireResult(boolean passed, int score, int passScore, List<QuestionScoreDetail> details) {
            this.passed = passed;
            this.score = score;
            this.passScore = passScore;
            this.details = details != null ? new ArrayList<>(details) : new ArrayList<>();
        }

        public boolean isPassed() { return passed; }
        public int getScore() { return score; }
        public int getPassScore() { return passScore; }
        public List<QuestionScoreDetail> getDetails() { return Collections.unmodifiableList(details); }

        public boolean isManualReviewRequired() {
            for (QuestionScoreDetail detail : details) {
                if (detail.isManualReview()) {
                    return true;
                }
            }
            return false;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("passed", passed);
            json.put("score", score);
            json.put("pass_score", passScore);
            json.put("manual_review_required", isManualReviewRequired());

            JSONArray detailArray = new JSONArray();
            for (QuestionScoreDetail detail : details) {
                detailArray.put(detail.toJson());
            }
            json.put("details", detailArray);
            return json;
        }
    }
}
