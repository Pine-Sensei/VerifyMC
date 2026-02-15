package team.kitemc.verifymc.domain.service;

import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;
import team.kitemc.verifymc.domain.repository.UserRepository;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

import java.util.List;
import java.util.Optional;

public class UserService {
    private final UserRepository userRepository;
    private final ConfigurationService configService;

    public UserService(UserRepository userRepository, ConfigurationService configService) {
        this.userRepository = userRepository;
        this.configService = configService;
    }

    public Optional<User> getUserByUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return Optional.empty();
        }
        return userRepository.findByUuid(uuid);
    }

    public Optional<User> getUserByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getPendingUsers() {
        return userRepository.findPending();
    }

    public PageResult<User> getUsersWithPagination(int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        List<User> users = userRepository.findWithPagination(page, pageSize);
        int total = userRepository.count();

        return PageResult.of(users, page, pageSize, total);
    }

    public PageResult<User> searchUsers(String query, int page, int pageSize) {
        if (query == null || query.trim().isEmpty()) {
            return PageResult.empty(page, pageSize);
        }
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        List<User> users = userRepository.search(query.trim(), page, pageSize);
        int total = users.size();

        return PageResult.of(users, page, pageSize, total);
    }

    public boolean registerUser(User user) {
        if (user == null) {
            return false;
        }

        ValidationResult validation = validateUser(user);
        if (!validation.isValid()) {
            return false;
        }

        return userRepository.save(user);
    }

    public boolean updateUserStatus(String uuid, UserStatus status) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }
        if (status == null) {
            return false;
        }

        Optional<User> userOpt = userRepository.findByUuid(uuid);
        if (userOpt.isEmpty()) {
            return false;
        }

        return userRepository.updateStatus(uuid, status);
    }

    public boolean updateUserPassword(String uuid, String password) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }
        if (password == null || password.isEmpty()) {
            return false;
        }

        Optional<User> userOpt = userRepository.findByUuid(uuid);
        if (userOpt.isEmpty()) {
            return false;
        }

        return userRepository.updatePassword(uuid, password);
    }

    public boolean updateUserEmail(String uuid, String email) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }
        if (email == null || email.isEmpty()) {
            return false;
        }

        if (!isValidEmail(email)) {
            return false;
        }

        Optional<User> userOpt = userRepository.findByUuid(uuid);
        if (userOpt.isEmpty()) {
            return false;
        }

        return userRepository.updateEmail(uuid, email);
    }

    public boolean deleteUser(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }

        Optional<User> userOpt = userRepository.findByUuid(uuid);
        if (userOpt.isEmpty()) {
            return false;
        }

        return userRepository.delete(uuid);
    }

    public int countUsersByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return 0;
        }
        return userRepository.countByEmail(email);
    }

    public boolean existsByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return userRepository.existsByUsername(username);
    }

    public int getMaxAccountsPerEmail() {
        return configService.getInt("max_accounts_per_email", 2);
    }

    public boolean canRegisterWithEmail(String email) {
        int maxAccounts = getMaxAccountsPerEmail();
        int currentCount = countUsersByEmail(email);
        return currentCount < maxAccounts;
    }

    private ValidationResult validateUser(User user) {
        ValidationResult.Builder builder = new ValidationResult.Builder();

        if (user.getUuid() == null || user.getUuid().isEmpty()) {
            builder.addError("UUID is required");
        }

        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            builder.addError("Username is required");
        } else if (existsByUsername(user.getUsername())) {
            builder.addError("Username already exists");
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            builder.addError("Email is required");
        } else if (!isValidEmail(user.getEmail())) {
            builder.addError("Invalid email format");
        } else if (!canRegisterWithEmail(user.getEmail())) {
            builder.addError("Email has reached maximum account limit");
        }

        return builder.build();
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    public void flush() {
        userRepository.flush();
    }
}
