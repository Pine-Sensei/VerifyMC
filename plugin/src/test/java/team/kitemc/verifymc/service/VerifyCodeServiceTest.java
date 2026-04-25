package team.kitemc.verifymc.service;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import team.kitemc.verifymc.core.ConfigManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerifyCodeServiceTest {
    @Test
    void registerEmailCodesUseConfiguredLengthAndCooldown() {
        Plugin plugin = mock(Plugin.class);
        FileConfiguration fileConfiguration = mock(FileConfiguration.class);
        ConfigManager configManager = mock(ConfigManager.class);

        when(plugin.getConfig()).thenReturn(fileConfiguration);
        when(fileConfiguration.getBoolean(eq("debug"), eq(false))).thenReturn(false);
        when(configManager.getEmailCodeLength()).thenReturn(8);
        when(configManager.getEmailCodeExpireSeconds()).thenReturn(120);
        when(configManager.getEmailCodeCooldownSeconds()).thenReturn(33);

        VerifyCodeService service = new VerifyCodeService(plugin, configManager);
        try {
            VerifyCodeService.CodeIssueResult result = service.issueCode(
                    VerifyCodeService.Channel.EMAIL,
                    VerifyCodeService.Purpose.REGISTER,
                    "user@example.com",
                    null);

            assertTrue(result.issued());
            assertEquals(8, result.code().length());
            assertEquals(33L, result.remainingSeconds());
        } finally {
            service.stop();
        }
    }
}
