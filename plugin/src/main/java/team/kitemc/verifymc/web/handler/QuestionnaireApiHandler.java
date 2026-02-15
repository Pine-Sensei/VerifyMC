package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import team.kitemc.verifymc.web.ApiResponse;
import team.kitemc.verifymc.web.WebResponseHelper;
import team.kitemc.verifymc.service.QuestionnaireService;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

public class QuestionnaireApiHandler implements HttpHandler {
    private final Plugin plugin;
    private final QuestionnaireService questionnaireService;
    private final BiFunction<String, String, String> messageResolver;
    
    public QuestionnaireApiHandler(Plugin plugin, QuestionnaireService questionnaireService, 
                                    BiFunction<String, String, String> messageResolver) {
        this.plugin = plugin;
        this.questionnaireService = questionnaireService;
        this.messageResolver = messageResolver;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            if (path.equals("/api/questionnaire") && method.equals("GET")) {
                handleGetQuestionnaire(exchange);
            } else if (path.equals("/api/submit-questionnaire") && method.equals("POST")) {
                handleSubmitQuestionnaire(exchange);
            } else {
                sendResponse(exchange, 404, ApiResponse.failure("Not found"));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, ApiResponse.failure("Internal server error: " + e.getMessage()));
        }
    }
    
    private void handleGetQuestionnaire(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String language = getQueryParam(query, "language", "en");
        
        try {
            JSONObject questionnaire = questionnaireService.getQuestionnaire(language);
            Map<String, Object> data = new HashMap<>();
            data.put("questions", questionnaire.opt("questions"));
            data.put("pass_score", questionnaireService.getPassScore());
            data.put("has_text_questions", questionnaireService.hasTextQuestions());
            sendResponse(exchange, 200, ApiResponse.success("ok", data));
        } catch (Exception e) {
            sendResponse(exchange, 500, ApiResponse.failure("Failed to get questionnaire"));
        }
    }
    
    private void handleSubmitQuestionnaire(HttpExchange exchange) throws IOException {
        JSONObject req = WebResponseHelper.readJson(exchange);
        String language = req.optString("language", "en");
        String requestId = UUID.randomUUID().toString();
        
        JSONObject answersJson = req.optJSONObject("answers");
        if (answersJson == null) {
            sendResponse(exchange, 400, ApiResponse.failure(messageResolver.apply("questionnaire.answers_required", language)));
            return;
        }
        
        try {
            JSONObject questionnaire = questionnaireService.getQuestionnaire(language);
            JSONArray questionDefs = questionnaire.optJSONArray("questions");
            
            if (questionDefs == null) {
                sendResponse(exchange, 400, ApiResponse.failure(messageResolver.apply("questionnaire.answers_required", language)));
                return;
            }
            
            Map<Integer, JSONObject> questionDefMap = new HashMap<>();
            for (int i = 0; i < questionDefs.length(); i++) {
                JSONObject questionDef = questionDefs.optJSONObject(i);
                if (questionDef != null) {
                    questionDefMap.put(questionDef.optInt("id", -1), questionDef);
                }
            }
            
            Map<Integer, QuestionnaireService.QuestionAnswer> answers = new HashMap<>();
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
                
                JSONArray selectedArray = answerObj.optJSONArray("selectedOptionIds");
                java.util.List<Integer> selectedOptionIds = new java.util.ArrayList<>();
                if (selectedArray != null) {
                    for (int i = 0; i < selectedArray.length(); i++) {
                        selectedOptionIds.add(selectedArray.getInt(i));
                    }
                }
                
                String textAnswer = answerObj.optString("textAnswer", "");
                answers.put(questionId, new QuestionnaireService.QuestionAnswer(answerType, selectedOptionIds, textAnswer));
            }
            
            QuestionnaireService.QuestionnaireResult result = questionnaireService.evaluateAnswers(answers);
            
            long submittedAt = System.currentTimeMillis();
            long expiresAt = submittedAt + 10 * 60 * 1000;
            String questionnaireToken = UUID.randomUUID().toString();
            
            Map<String, Object> data = new HashMap<>();
            data.put("passed", result.isPassed());
            data.put("score", result.getScore());
            data.put("pass_score", result.getPassScore());
            data.put("details", result.toJson().opt("details"));
            data.put("manual_review_required", result.toJson().optBoolean("manual_review_required", false));
            data.put("token", questionnaireToken);
            data.put("submitted_at", submittedAt);
            data.put("expires_at", expiresAt);
            
            String message = result.isPassed() ? 
                messageResolver.apply("questionnaire.passed", language).replace("{score}", String.valueOf(result.getScore())) : 
                messageResolver.apply("questionnaire.failed", language).replace("{score}", String.valueOf(result.getScore())).replace("{pass_score}", String.valueOf(result.getPassScore()));
            
            sendResponse(exchange, 200, ApiResponse.success(message, data));
            
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, ApiResponse.failure(e.getMessage()));
        } catch (Exception e) {
            sendResponse(exchange, 500, ApiResponse.failure("Failed to submit questionnaire: " + e.getMessage()));
        }
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, ApiResponse response) throws IOException {
        String json = response.toJson();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, json.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    private String getQueryParam(String query, String param, String defaultValue) {
        if (query == null) return defaultValue;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(param)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return defaultValue;
    }
}
