package team.kitemc.verifymc.web.handler;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.db.AuditDao;
import team.kitemc.verifymc.db.AuditRecord;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.service.AuthmeService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthFlowSupportTest {
    @Test
    void recordsAuditWhenAuthmeSyncFails() {
        PluginContext ctx = mock(PluginContext.class);
        UserDao userDao = mock(UserDao.class);
        AuthmeService authmeService = mock(AuthmeService.class);
        AuditDao auditDao = mock(AuditDao.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        Logger logger = Logger.getLogger("AuthFlowSupportTest");

        when(ctx.getUserDao()).thenReturn(userDao);
        when(ctx.getAuthmeService()).thenReturn(authmeService);
        when(ctx.getAuditDao()).thenReturn(auditDao);
        when(ctx.getPlugin()).thenReturn(plugin);
        when(plugin.getLogger()).thenReturn(logger);
        when(userDao.updateSharedPasswords(List.of("Alice", "Bob"), "new-password")).thenReturn(true);
        when(authmeService.isAuthmeEnabled()).thenReturn(true);
        when(authmeService.syncUserPasswordToAuthme("Alice", "new-password")).thenReturn(false);
        when(authmeService.syncUserPasswordToAuthme("Bob", "new-password")).thenReturn(true);

        AuthFlowSupport.SharedPasswordUpdateResult result = AuthFlowSupport.synchronizeSharedPasswords(
                ctx,
                List.of(
                        Map.of("username", "Alice"),
                        Map.of("username", "Bob")),
                "new-password");

        assertTrue(result.success());
        verify(auditDao).addAudit(any(AuditRecord.class));
    }

    @Test
    void skipsAuditWhenAuthmeSyncSucceeds() {
        PluginContext ctx = mock(PluginContext.class);
        UserDao userDao = mock(UserDao.class);
        AuthmeService authmeService = mock(AuthmeService.class);
        AuditDao auditDao = mock(AuditDao.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        Logger logger = Logger.getLogger("AuthFlowSupportTest");

        when(ctx.getUserDao()).thenReturn(userDao);
        when(ctx.getAuthmeService()).thenReturn(authmeService);
        when(ctx.getAuditDao()).thenReturn(auditDao);
        when(ctx.getPlugin()).thenReturn(plugin);
        when(plugin.getLogger()).thenReturn(logger);
        when(userDao.updateSharedPasswords(List.of("Alice"), "new-password")).thenReturn(true);
        when(authmeService.isAuthmeEnabled()).thenReturn(true);
        when(authmeService.syncUserPasswordToAuthme("Alice", "new-password")).thenReturn(true);

        AuthFlowSupport.SharedPasswordUpdateResult result = AuthFlowSupport.synchronizeSharedPasswords(
                ctx,
                List.of(Map.of("username", "Alice")),
                "new-password");

        assertTrue(result.success());
        verify(auditDao, never()).addAudit(any(AuditRecord.class));
    }
}
