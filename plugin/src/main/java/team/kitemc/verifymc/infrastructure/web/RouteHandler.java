package team.kitemc.verifymc.infrastructure.web;

public interface RouteHandler {
    void handle(RequestContext context) throws Exception;
}
