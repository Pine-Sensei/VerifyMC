package team.kitemc.verifymc.infrastructure.web;

public interface RouteHandler {
    ApiResponse handle(RequestContext context) throws Exception;
}
