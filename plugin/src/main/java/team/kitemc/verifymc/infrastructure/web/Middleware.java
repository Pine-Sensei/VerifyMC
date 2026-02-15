package team.kitemc.verifymc.infrastructure.web;

public interface Middleware {
    void handle(RequestContext ctx) throws Exception;
}
