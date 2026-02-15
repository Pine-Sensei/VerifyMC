package team.kitemc.verifymc.infrastructure.web;

public interface MiddlewareChain {
    void next() throws Exception;
}
