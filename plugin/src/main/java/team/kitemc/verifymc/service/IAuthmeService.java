package team.kitemc.verifymc.service;

public interface IAuthmeService {
    boolean isAuthmeEnabled();
    boolean isPasswordRequired();
    boolean isValidPassword(String password);
    boolean registerToAuthme(String username, String password);
    boolean unregisterFromAuthme(String username);
    boolean changePasswordInAuthme(String username, String newPassword);
    String encodePasswordForStorage(String plainOrEncodedPassword);
    void syncApprovedUsers();
}
