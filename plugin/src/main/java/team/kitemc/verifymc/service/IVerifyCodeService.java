package team.kitemc.verifymc.service;

/**
 * Verification code service interface for handling email verification codes.
 * Provides methods for generating, validating, and rate-limiting verification codes.
 */
public interface IVerifyCodeService {
    
    /**
     * Checks if a verification code can be sent to the specified email.
     * This method enforces rate limiting to prevent abuse.
     * @param email The email address to check
     * @return true if a code can be sent (not rate-limited)
     */
    boolean canSendCode(String email);
    
    /**
     * Gets the remaining cooldown time in seconds before a new code can be sent.
     * @param email The email address to check
     * @return The remaining cooldown seconds, or 0 if no cooldown is active
     */
    long getRemainingCooldownSeconds(String email);
    
    /**
     * Generates a new verification code for the specified key.
     * @param key The key (typically email) to associate with the code
     * @return The generated verification code
     */
    String generateCode(String key);
    
    /**
     * Validates a verification code for the specified key.
     * @param key The key associated with the code
     * @param code The code to validate
     * @return true if the code is valid and not expired
     */
    boolean checkCode(String key, String code);
}
