package team.kitemc.verifymc.db;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import team.kitemc.verifymc.util.PasswordUtil;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileUserDaoTest {
    @TempDir
    File tempDir;

    @Test
    void updatesSharedPasswordsAtomicallyWhenAllUsersExist() {
        FileUserDao userDao = createDao();
        try {
            assertTrue(userDao.registerUser("Alice", "shared@example.com", "approved", "old-password"));
            assertTrue(userDao.registerUser("Bob", "shared@example.com", "approved", "old-password"));

            assertTrue(userDao.updateSharedPasswords(List.of("Alice", "Bob"), "new-password"));

            assertTrue(PasswordUtil.verify("new-password", String.valueOf(userDao.getUserByUsername("Alice").get("password"))));
            assertTrue(PasswordUtil.verify("new-password", String.valueOf(userDao.getUserByUsername("Bob").get("password"))));
        } finally {
            userDao.close();
        }
    }

    @Test
    void doesNotApplySharedPasswordUpdateWhenAnyUserIsMissing() {
        FileUserDao userDao = createDao();
        try {
            assertTrue(userDao.registerUser("Alice", "shared@example.com", "approved", "old-password"));
            String beforePassword = String.valueOf(userDao.getUserByUsername("Alice").get("password"));

            assertFalse(userDao.updateSharedPasswords(List.of("Alice", "MissingUser"), "new-password"));

            Map<String, Object> user = userDao.getUserByUsername("Alice");
            assertTrue(PasswordUtil.verify("old-password", String.valueOf(user.get("password"))));
            assertFalse(beforePassword.isBlank());
        } finally {
            userDao.close();
        }
    }

    private FileUserDao createDao() {
        Plugin plugin = mock(Plugin.class);
        FileConfiguration config = mock(FileConfiguration.class);
        Logger logger = Logger.getLogger("FileUserDaoTest");
        when(plugin.getConfig()).thenReturn(config);
        when(config.getBoolean(eq("debug"), eq(false))).thenReturn(false);
        when(plugin.getDataFolder()).thenReturn(tempDir);
        when(plugin.getLogger()).thenReturn(logger);
        return new FileUserDao(new File(tempDir, "users.json"), plugin);
    }
}
