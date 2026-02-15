package team.kitemc.verifymc.web;

public class BusinessException extends RuntimeException {
    private final String code;
    
    public BusinessException(String message) {
        super(message);
        this.code = null;
    }
    
    public BusinessException(String message, String code) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = null;
    }
    
    public String getCode() {
        return code;
    }
}
