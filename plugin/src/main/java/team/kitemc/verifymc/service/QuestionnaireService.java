package team.kitemc.verifymc.service;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Questionnaire service for handling registration questionnaire
 * Supports single/multiple choice questions with scoring system
 */
public class QuestionnaireService {
    private final Plugin plugin;
    private final boolean debug;
    private FileConfiguration questionnaireConfig;
    
    public QuestionnaireService(Plugin plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
        loadQuestionnaireConfig();
    }
    
    /**
     * Load questionnaire configuration from file
     */
    private void loadQuestionnaireConfig() {
        File configFile = new File(plugin.getDataFolder(), "questionnaire.yml");
        if (!configFile.exists()) {
            // Create default questionnaire config
            createDefaultQuestionnaireConfig(configFile);
        }
        questionnaireConfig = YamlConfiguration.loadConfiguration(configFile);
        debugLog("Questionnaire configuration loaded");
    }
    
    /**
     * Create default questionnaire configuration file
     * @param configFile The config file to create
     */
    private void createDefaultQuestionnaireConfig(File configFile) {
        try {
            // Try to load from resources
            InputStream is = plugin.getResource("questionnaire.yml");
            if (is != null) {
                questionnaireConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
                questionnaireConfig.save(configFile);
            } else {
                // Create minimal default config
                questionnaireConfig = new YamlConfiguration();
                questionnaireConfig.set("enabled", false);
                questionnaireConfig.set("pass_score", 60);
                questionnaireConfig.set("auto_approve_on_pass", false);
                
                // Add example question
                List<Map<String, Object>> questions = new ArrayList<>();
                Map<String, Object> q1 = new HashMap<>();
                q1.put("id", 1);
                q1.put("question_zh", "您是如何得知本服务器的？");
                q1.put("question_en", "How did you hear about our server?");
                q1.put("type", "single_choice");
                
                List<Map<String, Object>> options = new ArrayList<>();
                Map<String, Object> opt1 = new HashMap<>();
                opt1.put("text_zh", "朋友推荐");
                opt1.put("text_en", "Friend recommendation");
                opt1.put("score", 10);
                options.add(opt1);
                
                Map<String, Object> opt2 = new HashMap<>();
                opt2.put("text_zh", "社交媒体");
                opt2.put("text_en", "Social media");
                opt2.put("score", 8);
                options.add(opt2);
                
                q1.put("options", options);
                questions.add(q1);
                
                questionnaireConfig.set("questions", questions);
                questionnaireConfig.save(configFile);
            }
            debugLog("Created default questionnaire config");
        } catch (Exception e) {
            debugLog("Failed to create questionnaire config: " + e.getMessage());
        }
    }
    
    /**
     * Check if questionnaire is enabled
     * @return true if questionnaire is enabled
     */
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("questionnaire.enabled", false) || 
               (questionnaireConfig != null && questionnaireConfig.getBoolean("enabled", false));
    }
    
    /**
     * Get minimum passing score
     * @return Minimum passing score
     */
    public int getPassScore() {
        int configScore = plugin.getConfig().getInt("questionnaire.pass_score", -1);
        if (configScore >= 0) return configScore;
        return questionnaireConfig != null ? questionnaireConfig.getInt("pass_score", 60) : 60;
    }
    
    /**
     * Check if auto-approve is enabled when user passes
     * @return true if auto-approve on pass is enabled
     */
    public boolean isAutoApproveOnPass() {
        return plugin.getConfig().getBoolean("questionnaire.auto_approve_on_pass", false) ||
               (questionnaireConfig != null && questionnaireConfig.getBoolean("auto_approve_on_pass", false));
    }
    
    /**
     * Get questionnaire as JSON for frontend
     * @param language Language code (zh or en)
     * @return JSON object containing questionnaire data
     */
    public JSONObject getQuestionnaire(String language) {
        JSONObject result = new JSONObject();
        result.put("enabled", isEnabled());
        result.put("pass_score", getPassScore());
        
        if (!isEnabled() || questionnaireConfig == null) {
            result.put("questions", new JSONArray());
            return result;
        }
        
        List<?> questionsList = questionnaireConfig.getList("questions");
        JSONArray questionsArray = new JSONArray();
        
        if (questionsList != null) {
            for (Object qObj : questionsList) {
                if (qObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> questionMap = (Map<String, Object>) qObj;
                    JSONObject question = new JSONObject();
                    
                    question.put("id", questionMap.get("id"));
                    
                    // Get question text based on language
                    String questionText = "zh".equals(language) ? 
                        (String) questionMap.get("question_zh") : 
                        (String) questionMap.get("question_en");
                    if (questionText == null) {
                        questionText = (String) questionMap.getOrDefault("question", "");
                    }
                    question.put("question", questionText);
                    question.put("type", questionMap.getOrDefault("type", "single_choice"));
                    
                    // Get options
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> optionsList = (List<Map<String, Object>>) questionMap.get("options");
                    JSONArray optionsArray = new JSONArray();
                    
                    if (optionsList != null) {
                        int optionIndex = 0;
                        for (Map<String, Object> optMap : optionsList) {
                            JSONObject option = new JSONObject();
                            option.put("id", optionIndex++);
                            
                            // Get option text based on language
                            String optText = "zh".equals(language) ?
                                (String) optMap.get("text_zh") :
                                (String) optMap.get("text_en");
                            if (optText == null) {
                                optText = (String) optMap.getOrDefault("text", "");
                            }
                            option.put("text", optText);
                            // Don't expose score to frontend for security
                            optionsArray.put(option);
                        }
                    }
                    question.put("options", optionsArray);
                    questionsArray.put(question);
                }
            }
        }
        
        result.put("questions", questionsArray);
        return result;
    }
    
    /**
     * Calculate score based on answers
     * @param answers Map of question ID to selected option ID(s)
     * @return Calculated score
     */
    public int calculateScore(Map<Integer, List<Integer>> answers) {
        if (!isEnabled() || questionnaireConfig == null) {
            return 100; // If questionnaire is disabled, return full score
        }
        
        int totalScore = 0;
        List<?> questionsList = questionnaireConfig.getList("questions");
        
        if (questionsList != null) {
            for (Object qObj : questionsList) {
                if (qObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> questionMap = (Map<String, Object>) qObj;
                    int questionId = ((Number) questionMap.get("id")).intValue();
                    
                    List<Integer> selectedOptions = answers.get(questionId);
                    if (selectedOptions == null || selectedOptions.isEmpty()) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> optionsList = (List<Map<String, Object>>) questionMap.get("options");
                    if (optionsList != null) {
                        for (int optionId : selectedOptions) {
                            if (optionId >= 0 && optionId < optionsList.size()) {
                                Map<String, Object> option = optionsList.get(optionId);
                                Object scoreObj = option.get("score");
                                if (scoreObj instanceof Number) {
                                    totalScore += ((Number) scoreObj).intValue();
                                }
                            }
                        }
                    }
                }
            }
        }
        
        debugLog("Calculated questionnaire score: " + totalScore);
        return totalScore;
    }
    
    /**
     * Check if user passed the questionnaire
     * @param answers Map of question ID to selected option ID(s)
     * @return QuestionnaireResult containing pass status and score
     */
    public QuestionnaireResult evaluateAnswers(Map<Integer, List<Integer>> answers) {
        int score = calculateScore(answers);
        int passScore = getPassScore();
        boolean passed = score >= passScore;
        
        debugLog("Questionnaire evaluation: score=" + score + ", passScore=" + passScore + ", passed=" + passed);
        
        return new QuestionnaireResult(passed, score, passScore);
    }
    
    /**
     * Reload questionnaire configuration
     */
    public void reload() {
        loadQuestionnaireConfig();
    }
    
    private void debugLog(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] QuestionnaireService: " + msg);
        }
    }
    
    /**
     * Result class for questionnaire evaluation
     */
    public static class QuestionnaireResult {
        private final boolean passed;
        private final int score;
        private final int passScore;
        
        public QuestionnaireResult(boolean passed, int score, int passScore) {
            this.passed = passed;
            this.score = score;
            this.passScore = passScore;
        }
        
        public boolean isPassed() {
            return passed;
        }
        
        public int getScore() {
            return score;
        }
        
        public int getPassScore() {
            return passScore;
        }
        
        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("passed", passed);
            json.put("score", score);
            json.put("pass_score", passScore);
            return json;
        }
    }
}

