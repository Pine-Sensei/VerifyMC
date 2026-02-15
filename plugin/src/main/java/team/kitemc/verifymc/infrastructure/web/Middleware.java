package team.kitemc.verifymc.infrastructure.web;

public interface Middleware {
    boolean intercept(RequestContext context) throws Exception;
}
