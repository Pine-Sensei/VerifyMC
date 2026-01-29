package team.kitemc.verifymc.service;

import org.bukkit.plugin.Plugin;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Captcha service for generating and validating image-based captcha
 * Supports both math expressions and text captcha types
 */
public class CaptchaService {
    private final Plugin plugin;
    private final boolean debug;
    private final SecureRandom random = new SecureRandom();
    
    // Store captcha tokens and their answers: token -> CaptchaData
    private final Map<String, CaptchaData> captchaStore = new ConcurrentHashMap<>();
    
    // Captcha configuration
    private static final int IMAGE_WIDTH = 150;
    private static final int IMAGE_HEIGHT = 50;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    
    // Cleanup interval in milliseconds (5 minutes)
    private static final long CLEANUP_INTERVAL = 300000;
    
    public CaptchaService(Plugin plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
        startCleanupTask();
    }
    
    /**
     * Generate a new captcha and return the result
     * @return CaptchaResult containing token and base64 image
     */
    public CaptchaResult generateCaptcha() {
        String type = plugin.getConfig().getString("captcha.type", "math");
        int length = plugin.getConfig().getInt("captcha.length", 4);
        int expireSeconds = plugin.getConfig().getInt("captcha.expire_seconds", 300);
        
        String answer;
        String displayText;
        
        if ("math".equalsIgnoreCase(type)) {
            // Generate math expression
            int num1 = random.nextInt(20) + 1;
            int num2 = random.nextInt(10) + 1;
            int operator = random.nextInt(2); // 0: +, 1: -
            
            if (operator == 0) {
                displayText = num1 + " + " + num2 + " = ?";
                answer = String.valueOf(num1 + num2);
            } else {
                // Ensure result is positive
                if (num1 < num2) {
                    int temp = num1;
                    num1 = num2;
                    num2 = temp;
                }
                displayText = num1 + " - " + num2 + " = ?";
                answer = String.valueOf(num1 - num2);
            }
        } else {
            // Generate text captcha
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            answer = sb.toString();
            displayText = answer;
        }
        
        // Generate token
        String token = generateToken();
        
        // Store captcha data
        long expireTime = System.currentTimeMillis() + (expireSeconds * 1000L);
        captchaStore.put(token, new CaptchaData(answer, expireTime));
        
        debugLog("Generated captcha: token=" + token + ", answer=" + answer);
        
        // Generate image
        String imageBase64 = generateCaptchaImage(displayText);
        
        return new CaptchaResult(token, imageBase64);
    }
    
    /**
     * Validate captcha answer
     * @param token Captcha token
     * @param answer User's answer
     * @return true if answer is correct
     */
    public boolean validateCaptcha(String token, String answer) {
        if (token == null || answer == null) {
            debugLog("Captcha validation failed: token or answer is null");
            return false;
        }
        
        CaptchaData data = captchaStore.get(token);
        if (data == null) {
            debugLog("Captcha validation failed: token not found - " + token);
            return false;
        }
        
        // Check expiration
        if (System.currentTimeMillis() > data.expireTime) {
            captchaStore.remove(token);
            debugLog("Captcha validation failed: token expired - " + token);
            return false;
        }
        
        // Validate answer (case insensitive)
        boolean valid = data.answer.equalsIgnoreCase(answer.trim());
        
        // Remove token after validation (one-time use)
        captchaStore.remove(token);
        
        debugLog("Captcha validation: token=" + token + ", answer=" + answer + ", expected=" + data.answer + ", valid=" + valid);
        
        return valid;
    }
    
    /**
     * Check if captcha is enabled
     * @return true if captcha is enabled in auth_methods
     */
    public boolean isCaptchaEnabled() {
        return plugin.getConfig().getStringList("auth_methods").contains("captcha");
    }
    
    /**
     * Generate captcha image as base64 string
     * @param text Text to display
     * @return Base64 encoded PNG image
     */
    private String generateCaptchaImage(String text) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Background with gradient
        GradientPaint gradient = new GradientPaint(0, 0, new Color(240, 240, 250), 
                                                   IMAGE_WIDTH, IMAGE_HEIGHT, new Color(220, 225, 240));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        
        // Draw noise lines
        g2d.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 6; i++) {
            g2d.setColor(new Color(
                random.nextInt(100) + 100,
                random.nextInt(100) + 100,
                random.nextInt(100) + 100,
                100
            ));
            int x1 = random.nextInt(IMAGE_WIDTH);
            int y1 = random.nextInt(IMAGE_HEIGHT);
            int x2 = random.nextInt(IMAGE_WIDTH);
            int y2 = random.nextInt(IMAGE_HEIGHT);
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        // Draw noise dots
        for (int i = 0; i < 50; i++) {
            g2d.setColor(new Color(
                random.nextInt(150) + 50,
                random.nextInt(150) + 50,
                random.nextInt(150) + 50
            ));
            int x = random.nextInt(IMAGE_WIDTH);
            int y = random.nextInt(IMAGE_HEIGHT);
            g2d.fillOval(x, y, 2, 2);
        }
        
        // Draw text
        Font font = new Font("Arial", Font.BOLD, 28);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = (IMAGE_WIDTH - textWidth) / 2;
        int y = (IMAGE_HEIGHT + fm.getAscent() - fm.getDescent()) / 2;
        
        // Draw text with slight color variations for each character
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            g2d.setColor(new Color(
                random.nextInt(80) + 20,
                random.nextInt(80) + 20,
                random.nextInt(80) + 100
            ));
            
            // Slight rotation for each character
            double angle = (random.nextDouble() - 0.5) * 0.3;
            int charX = x + fm.stringWidth(text.substring(0, i));
            int charY = y + random.nextInt(6) - 3;
            
            g2d.rotate(angle, charX, charY);
            g2d.drawString(String.valueOf(c), charX, charY);
            g2d.rotate(-angle, charX, charY);
        }
        
        g2d.dispose();
        
        // Convert to base64
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            debugLog("Failed to generate captcha image: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Generate a unique token
     * @return Token string
     */
    private String generateToken() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    /**
     * Start background cleanup task for expired captchas
     */
    private void startCleanupTask() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(CLEANUP_INTERVAL);
                    long currentTime = System.currentTimeMillis();
                    int removed = 0;
                    for (Map.Entry<String, CaptchaData> entry : captchaStore.entrySet()) {
                        if (currentTime > entry.getValue().expireTime) {
                            captchaStore.remove(entry.getKey());
                            removed++;
                        }
                    }
                    if (removed > 0) {
                        debugLog("Cleaned up " + removed + " expired captcha tokens");
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "CaptchaCleanup").start();
    }
    
    private void debugLog(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] CaptchaService: " + msg);
        }
    }
    
    /**
     * Internal class to store captcha data
     */
    private static class CaptchaData {
        final String answer;
        final long expireTime;
        
        CaptchaData(String answer, long expireTime) {
            this.answer = answer;
            this.expireTime = expireTime;
        }
    }
    
    /**
     * Result class for captcha generation
     */
    public static class CaptchaResult {
        private final String token;
        private final String imageBase64;
        
        public CaptchaResult(String token, String imageBase64) {
            this.token = token;
            this.imageBase64 = imageBase64;
        }
        
        public String getToken() {
            return token;
        }
        
        public String getImageBase64() {
            return imageBase64;
        }
    }
}

