package team.kitemc.verifymc.service;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountSelectionServiceTest {
    @Test
    void consumesValidTokenOnlyOnce() {
        AccountSelectionService service = new AccountSelectionService(300000L);
        String token = service.issueToken(
                AccountSelectionService.Purpose.LOGIN,
                "email",
                "user@example.com",
                List.of("Alice", "Bob"));

        AccountSelectionService.ConsumeResult result = service.consume(
                token,
                AccountSelectionService.Purpose.LOGIN,
                "Alice");

        assertTrue(result.valid());
        assertEquals("Alice", result.username());
        assertFalse(service.consume(token, AccountSelectionService.Purpose.LOGIN, "Alice").valid());
    }

    @Test
    void rejectsWrongPurposeAndExpiredToken() throws InterruptedException {
        AccountSelectionService service = new AccountSelectionService(80L);
        String token = service.issueToken(
                AccountSelectionService.Purpose.FORGOT_PASSWORD,
                "phone",
                "+8613800138000",
                List.of("Alice"));

        assertFalse(service.consume(token, AccountSelectionService.Purpose.LOGIN, "Alice").valid());

        String expiringToken = service.issueToken(
                AccountSelectionService.Purpose.LOGIN,
                "email",
                "shared@example.com",
                List.of("Alice"));
        Thread.sleep(90L);

        assertFalse(service.consume(expiringToken, AccountSelectionService.Purpose.LOGIN, "Alice").valid());
    }
}
