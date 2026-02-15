package team.kitemc.verifymc.service;

import team.kitemc.verifymc.registration.RegistrationOutcome;
import team.kitemc.verifymc.web.RegistrationRequest;
import team.kitemc.verifymc.web.RegistrationValidationResult;

/**
 * Registration service interface for handling user registration operations.
 * Provides methods for validating and processing user registration requests.
 */
public interface IRegistrationService {
    
    /**
     * Validates a registration request.
     * @param request The registration request to validate
     * @return A validation result containing any errors
     */
    RegistrationValidationResult validateRegistration(RegistrationRequest request);
    
    /**
     * Processes a registration request and creates a new user.
     * @param request The registration request to process
     * @return The outcome of the registration process
     */
    RegistrationOutcome register(RegistrationRequest request);
    
    /**
     * Determines if a registration should be automatically approved.
     * @param manualReviewRequired Whether manual review is required
     * @param registerAutoApprove Whether auto-approve is enabled in config
     * @return true if the registration should be auto-approved
     */
    boolean shouldAutoApprove(boolean manualReviewRequired, boolean registerAutoApprove);
}
