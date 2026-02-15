package team.kitemc.verifymc.service.impl;

import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.registration.RegistrationOutcome;
import team.kitemc.verifymc.registration.RegistrationOutcomeResolver;
import team.kitemc.verifymc.service.IRegistrationService;
import team.kitemc.verifymc.service.VerifyCodeService;
import team.kitemc.verifymc.service.AuthmeService;
import team.kitemc.verifymc.service.DiscordService;
import team.kitemc.verifymc.web.RegistrationRequest;
import team.kitemc.verifymc.web.RegistrationValidationResult;
import team.kitemc.verifymc.web.BusinessException;
import team.kitemc.verifymc.web.ErrorCode;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class RegistrationServiceImpl implements IRegistrationService {
    private static final Logger LOGGER = Logger.getLogger(RegistrationServiceImpl.class.getName());
    
    private final Plugin plugin;
    private final UserDao userDao;
    private final VerifyCodeService verifyCodeService;
    private final AuthmeService authmeService;
    private final DiscordService discordService;
    private final RegistrationOutcomeResolver outcomeResolver;
    
    private List<String> authMethods;
    private int maxAccountsPerEmail;
    private boolean registerAutoApprove;
    private BiPredicate<String, String> usernameValidator;
    private Function<String, Boolean> usernameCaseConflictChecker;
    private Function<String, Boolean> emailValidator;
    private Function<String, Boolean> uuidValidator;
    
    public RegistrationServiceImpl(Plugin plugin, UserDao userDao, VerifyCodeService verifyCodeService,
                                   AuthmeService authmeService, DiscordService discordService,
                                   RegistrationOutcomeResolver outcomeResolver) {
        this.plugin = plugin;
        this.userDao = userDao;
        this.verifyCodeService = verifyCodeService;
        this.authmeService = authmeService;
        this.discordService = discordService;
        this.outcomeResolver = outcomeResolver;
    }
    
    public void configure(List<String> authMethods, int maxAccountsPerEmail, boolean registerAutoApprove,
                          BiPredicate<String, String> usernameValidator,
                          Function<String, Boolean> usernameCaseConflictChecker,
                          Function<String, Boolean> emailValidator,
                          Function<String, Boolean> uuidValidator) {
        this.authMethods = authMethods;
        this.maxAccountsPerEmail = maxAccountsPerEmail;
        this.registerAutoApprove = registerAutoApprove;
        this.usernameValidator = usernameValidator;
        this.usernameCaseConflictChecker = usernameCaseConflictChecker;
        this.emailValidator = emailValidator;
        this.uuidValidator = uuidValidator;
    }
    
    @Override
    public RegistrationValidationResult validateRegistration(RegistrationRequest request) {
        if (authmeService.isAuthmeEnabled() && authmeService.isPasswordRequired()) {
            if (request.password() == null || request.password().trim().isEmpty()) {
                return RegistrationValidationResult.reject("register.password_required");
            }
            if (!authmeService.isValidPassword(request.password())) {
                String passwordRegex = plugin.getConfig().getString("authme.password_regex", "^[a-zA-Z0-9_]{8,26}$");
                return RegistrationValidationResult.reject("register.invalid_password", 
                    new org.json.JSONObject().put("regex", passwordRegex));
            }
        }
        
        if (request.normalizedUsername() == null || request.normalizedUsername().trim().isEmpty()) {
            return RegistrationValidationResult.reject("register.invalid_username");
        }
        
        if (userDao.getUserByUsername(request.normalizedUsername()) != null) {
            return RegistrationValidationResult.reject("register.username_exists");
        }
        
        if (usernameValidator != null && !usernameValidator.test(request.normalizedUsername(), request.platform())) {
            return RegistrationValidationResult.reject("username.invalid");
        }
        
        if (usernameCaseConflictChecker != null && usernameCaseConflictChecker.apply(request.normalizedUsername())) {
            return RegistrationValidationResult.reject("username.case_conflict");
        }
        
        if (request.email() == null || request.email().isEmpty()) {
            return RegistrationValidationResult.reject("register.invalid_email");
        }
        
        int emailCount = userDao.countUsersByEmail(request.email());
        if (emailCount >= maxAccountsPerEmail) {
            return RegistrationValidationResult.reject("register.email_limit");
        }
        
        if (emailValidator != null && !emailValidator.apply(request.email())) {
            return RegistrationValidationResult.reject("register.invalid_email");
        }
        
        if (request.uuid() == null || request.uuid().isEmpty()) {
            return RegistrationValidationResult.reject("register.invalid_uuid");
        }
        
        if (uuidValidator != null && !uuidValidator.apply(request.uuid())) {
            return RegistrationValidationResult.reject("register.invalid_uuid");
        }
        
        return RegistrationValidationResult.pass();
    }
    
    @Override
    public RegistrationOutcome register(RegistrationRequest request) {
        RegistrationValidationResult validationResult = validateRegistration(request);
        if (!validationResult.passed()) {
            return RegistrationOutcome.FAILED;
        }
        
        boolean useEmail = authMethods != null && authMethods.contains("email");
        if (useEmail || authMethods == null || authMethods.isEmpty()) {
            if (!verifyCodeService.checkCode(request.email(), request.code())) {
                throw new BusinessException("Invalid verification code", ErrorCode.INVALID_VERIFY_CODE.getCode());
            }
        }
        
        if (discordService != null && discordService.isRequired() && !discordService.isLinked(request.normalizedUsername())) {
            throw new BusinessException("Discord linking required", ErrorCode.DISCORD_REQUIRED.getCode());
        }
        
        boolean questionnaireEnabled = plugin.getConfig().getBoolean("questionnaire.enabled", false);
        boolean manualReviewRequired = questionnaireEnabled && request.questionnaire() != null;
        boolean autoApprove = shouldAutoApprove(manualReviewRequired, registerAutoApprove);
        
        String status = autoApprove ? "approved" : "pending";
        
        boolean registered;
        if (authmeService.isAuthmeEnabled() && request.password() != null && !request.password().trim().isEmpty()) {
            String storedPassword = authmeService.encodePasswordForStorage(request.password());
            registered = userDao.registerUser(request.uuid(), request.normalizedUsername(), 
                request.email(), status, storedPassword);
        } else {
            registered = userDao.registerUser(request.uuid(), request.normalizedUsername(), 
                request.email(), status);
        }
        
        if (!registered) {
            LOGGER.warning("Failed to register user: " + request.normalizedUsername());
            return RegistrationOutcome.FAILED;
        }
        
        String discordId = discordService != null ? discordService.getLinkedDiscordId(request.normalizedUsername()) : null;
        if (discordId != null) {
            userDao.updateUserDiscordId(request.uuid(), discordId);
        }
        
        if (autoApprove && authmeService.isAuthmeEnabled() && request.password() != null && !request.password().trim().isEmpty()) {
            authmeService.registerToAuthme(request.normalizedUsername(), request.password());
        }
        
        LOGGER.info("Registered user: " + request.normalizedUsername() + " with status: " + status);
        
        return autoApprove ? RegistrationOutcome.SUCCESS_WHITELISTED : RegistrationOutcome.SUCCESS_PENDING;
    }
    
    @Override
    public boolean shouldAutoApprove(boolean manualReviewRequired, boolean registerAutoApprove) {
        return registerAutoApprove && !manualReviewRequired;
    }
}
