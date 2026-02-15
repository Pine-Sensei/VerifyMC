package team.kitemc.verifymc.domain.service;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class RegistrationService {
    private final Plugin plugin;
    private final UserService userService;
    private final ConfigurationService configService;

    public RegistrationService(Plugin plugin, UserService userService, ConfigurationService configService) {
        this.plugin = plugin;
        this.userService = userService;
        this.configService = configService;
    }

    public RegistrationResult register(RegistrationRequest request) {
        if (request == null) {
            return RegistrationResult.failure("Invalid registration request");
        }

        ValidationResult validation = validateRequest(request);
        if (!validation.isValid()) {
            return RegistrationResult.failure(validation.getFirstError());
        }

        UserStatus initialStatus = determineInitialStatus(false, isManualReviewRequired());

        User user = new User.Builder()
                .uuid(request.getUuid())
                .username(request.getNormalizedUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .status(initialStatus)
                .regTime(System.currentTimeMillis())
                .build();

        boolean registered = userService.registerUser(user);
        if (!registered) {
            return RegistrationResult.failure("Failed to register user");
        }

        String message = initialStatus == UserStatus.APPROVED 
                ? "Registration successful. You have been whitelisted." 
                : "Registration successful. Your application is pending review.";

        String messageKey = initialStatus == UserStatus.APPROVED 
                ? "register.success_whitelisted" 
                : "register.success_pending";

        return RegistrationResult.success(user, initialStatus, message, messageKey);
    }

    public ValidationResult validateRequest(RegistrationRequest request) {
        ValidationResult.Builder builder = new ValidationResult.Builder();

        if (request == null) {
            builder.addError("Request cannot be null");
            return builder.build();
        }

        if (request.getUuid() == null || request.getUuid().isEmpty()) {
            builder.addError("UUID is required");
        } else if (!isValidUuid(request.getUuid())) {
            builder.addError("Invalid UUID format");
        }

        if (request.getNormalizedUsername() == null || request.getNormalizedUsername().isEmpty()) {
            builder.addError("Username is required");
        } else {
            if (!isValidUsername(request.getNormalizedUsername(), request.getPlatform())) {
                builder.addError("Invalid username format");
            }
            if (!checkUsernameAvailability(request.getNormalizedUsername())) {
                builder.addError("Username already exists");
            }
        }

        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            builder.addError("Email is required");
        } else {
            if (!isValidEmail(request.getEmail())) {
                builder.addError("Invalid email format");
            }
            if (!checkEmailAvailability(request.getEmail())) {
                builder.addError("Email has reached maximum account limit");
            }
            if (!isEmailDomainAllowed(request.getEmail())) {
                builder.addError("Email domain not allowed");
            }
        }

        if (isPasswordRequired() && (request.getPassword() == null || request.getPassword().isEmpty())) {
            builder.addError("Password is required");
        }

        return builder.build();
    }

    public boolean checkUsernameAvailability(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return !userService.existsByUsername(username);
    }

    public boolean checkEmailAvailability(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return userService.canRegisterWithEmail(email);
    }

    public UserStatus determineInitialStatus(boolean questionnairePassed, boolean manualReviewRequired) {
        boolean autoApprove = configService.getBoolean("register.auto_approve", false);

        if (manualReviewRequired) {
            if (autoApprove && questionnairePassed) {
                return UserStatus.APPROVED;
            }
            return UserStatus.PENDING;
        }

        if (autoApprove || questionnairePassed) {
            return UserStatus.APPROVED;
        }

        return UserStatus.PENDING;
    }

    private boolean isValidUuid(String uuid) {
        try {
            java.util.UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isValidUsername(String username, String platform) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        boolean bedrockEnabled = configService.getBoolean("bedrock.enabled", false);
        String bedrockPrefix = configService.getString("bedrock.prefix", ".");

        if (bedrockEnabled && "bedrock".equalsIgnoreCase(platform) && username.startsWith(bedrockPrefix)) {
            String bedrockRegex = configService.getString("bedrock.username_regex", "^\\.[a-zA-Z0-9_\\s]{3,16}$");
            return username.matches(bedrockRegex);
        }

        String regex = configService.getString("username_regex", "^[a-zA-Z0-9_-]{3,16}$");
        return username.matches(regex);
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    private boolean isEmailDomainAllowed(String email) {
        boolean domainWhitelistEnabled = configService.getBoolean("enable_email_domain_whitelist", true);
        if (!domainWhitelistEnabled) {
            return true;
        }

        List<String> allowedDomains = configService.getStringList("email_domain_whitelist");
        if (allowedDomains.isEmpty()) {
            return true;
        }

        String domain = email.contains("@") ? email.substring(email.indexOf('@') + 1) : "";
        return allowedDomains.contains(domain);
    }

    private boolean isPasswordRequired() {
        boolean authmeEnabled = configService.getBoolean("authme.enabled", false);
        boolean passwordRequired = configService.getBoolean("authme.password_required", false);
        return authmeEnabled && passwordRequired;
    }

    private boolean isManualReviewRequired() {
        return configService.getBoolean("questionnaire.enabled", false) 
                && configService.getBoolean("questionnaire.manual_review", false);
    }

    public void syncToWhitelist(String username) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist add " + username);
        });
    }
}
