package team.kitemc.verifymc.infrastructure.persistence;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class QuestionnaireRepository {

    private final Plugin plugin;
    private final boolean debug;
    private FileConfiguration questionnaireConfig;
    private final List<QuestionDefinition> questions = new CopyOnWriteArrayList<>();

    public QuestionnaireRepository(Plugin plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
        load();
    }

    public synchronized void load() {
        File configFile = new File(plugin.getDataFolder(), "questionnaire.yml");
        if (!configFile.exists()) {
            createDefaultQuestionnaireConfig(configFile);
        }
        questionnaireConfig = YamlConfiguration.loadConfiguration(configFile);
        parseQuestions();
        debugLog("Questionnaire configuration loaded, " + questions.size() + " questions");
    }

    private void createDefaultQuestionnaireConfig(File configFile) {
        try {
            InputStream is = plugin.getResource("questionnaire.yml");
            if (is != null) {
                questionnaireConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
                questionnaireConfig.save(configFile);
            } else {
                questionnaireConfig = new YamlConfiguration();
                questionnaireConfig.set("questions", Collections.emptyList());
                questionnaireConfig.save(configFile);
            }
            debugLog("Created default questionnaire config");
        } catch (Exception e) {
            debugLog("Failed to create questionnaire config: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void parseQuestions() {
        questions.clear();
        if (questionnaireConfig == null) {
            return;
        }

        List<?> questionsList = questionnaireConfig.getList("questions");
        if (questionsList == null) {
            return;
        }

        for (Object qObj : questionsList) {
            if (!(qObj instanceof Map)) {
                continue;
            }

            Map<String, Object> questionMap = (Map<String, Object>) qObj;
            QuestionDefinition question = parseQuestionDefinition(questionMap);
            if (question != null) {
                questions.add(question);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private QuestionDefinition parseQuestionDefinition(Map<String, Object> map) {
        try {
            int id = ((Number) map.get("id")).intValue();
            String type = String.valueOf(map.getOrDefault("type", "single_choice"));
            String questionEn = (String) map.get("question_en");
            String questionZh = (String) map.get("question_zh");
            boolean required = Boolean.TRUE.equals(map.get("required"));
            int maxScore = map.containsKey("max_score") ? ((Number) map.get("max_score")).intValue() : 0;
            String scoringRule = (String) map.get("scoring_rule");

            Map<String, Object> input = (Map<String, Object>) map.get("input");
            List<Map<String, Object>> optionsList = (List<Map<String, Object>>) map.get("options");

            List<QuestionOption> options = new ArrayList<>();
            if (optionsList != null) {
                for (Map<String, Object> optMap : optionsList) {
                    String optTextEn = (String) optMap.get("text_en");
                    String optTextZh = (String) optMap.get("text_zh");
                    int score = optMap.containsKey("score") ? ((Number) optMap.get("score")).intValue() : 0;
                    options.add(new QuestionOption(optTextEn, optTextZh, score));
                }
            }

            return new QuestionDefinition(id, type, questionEn, questionZh, required, input, options, maxScore, scoringRule);
        } catch (Exception e) {
            debugLog("Failed to parse question definition: " + e.getMessage());
            return null;
        }
    }

    public List<QuestionDefinition> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public Optional<QuestionDefinition> getQuestionById(int id) {
        return questions.stream()
                .filter(q -> q.getId() == id)
                .findFirst();
    }

    public FileConfiguration getRawConfig() {
        return questionnaireConfig;
    }

    private void debugLog(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] QuestionnaireRepository: " + msg);
        }
    }

    public static class QuestionDefinition {
        private final int id;
        private final String type;
        private final String questionEn;
        private final String questionZh;
        private final boolean required;
        private final Map<String, Object> input;
        private final List<QuestionOption> options;
        private final int maxScore;
        private final String scoringRule;

        public QuestionDefinition(int id, String type, String questionEn, String questionZh,
                                  boolean required, Map<String, Object> input,
                                  List<QuestionOption> options, int maxScore, String scoringRule) {
            this.id = id;
            this.type = type != null ? type : "single_choice";
            this.questionEn = questionEn != null ? questionEn : "";
            this.questionZh = questionZh != null ? questionZh : "";
            this.required = required;
            this.input = input;
            this.options = options != null ? new ArrayList<>(options) : Collections.emptyList();
            this.maxScore = maxScore;
            this.scoringRule = scoringRule != null ? scoringRule : "";
        }

        public int getId() { return id; }
        public String getType() { return type; }
        public String getQuestionEn() { return questionEn; }
        public String getQuestionZh() { return questionZh; }
        public boolean isRequired() { return required; }
        public Map<String, Object> getInput() { return input; }
        public List<QuestionOption> getOptions() { return Collections.unmodifiableList(options); }
        public int getMaxScore() { return maxScore; }
        public String getScoringRule() { return scoringRule; }

        public boolean isTextType() {
            return "text".equalsIgnoreCase(type);
        }

        public boolean isChoiceType() {
            return "single_choice".equalsIgnoreCase(type) || "multiple_choice".equalsIgnoreCase(type);
        }

        public int calculateMaxScore() {
            if (isTextType()) {
                return maxScore > 0 ? maxScore : 20;
            }

            if (maxScore > 0) {
                return maxScore;
            }

            int total = 0;
            for (QuestionOption opt : options) {
                total += opt.getScore();
            }
            return Math.max(1, total);
        }
    }

    public static class QuestionOption {
        private final String textEn;
        private final String textZh;
        private final int score;

        public QuestionOption(String textEn, String textZh, int score) {
            this.textEn = textEn != null ? textEn : "";
            this.textZh = textZh != null ? textZh : "";
            this.score = score;
        }

        public String getTextEn() { return textEn; }
        public String getTextZh() { return textZh; }
        public int getScore() { return score; }
    }
}
