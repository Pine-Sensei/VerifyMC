package team.kitemc.verifymc.domain.repository;

import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    boolean save(User user);

    Optional<User> findByUuid(String uuid);

    Optional<User> findByUsername(String username);

    List<User> findByEmail(String email);

    Optional<User> findByDiscordId(String discordId);

    List<User> findAll();

    List<User> findPending();

    List<User> findWithPagination(int page, int pageSize);

    List<User> findApprovedWithPagination(int page, int pageSize);

    List<User> search(String query, int page, int pageSize);

    int count();

    int countApproved();

    int countByEmail(String email);

    boolean updateStatus(String uuid, UserStatus status);

    boolean updatePassword(String uuid, String password);

    boolean updateEmail(String uuid, String email);

    boolean updateDiscordId(String uuid, String discordId);

    boolean delete(String uuid);

    boolean existsByUsername(String username);

    boolean existsByDiscordId(String discordId);

    void flush();
}
