package team.kitemc.verifymc.infrastructure.web;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HttpServerFactory {
    private static final int DEFAULT_BACKLOG = 0;
    private static final int DEFAULT_THREAD_POOL_SIZE = 10;

    public HttpServer create(int port, int backlog) throws IOException {
        InetSocketAddress address = new InetSocketAddress(port);
        HttpServer server = HttpServer.create(address, backlog);
        configureServer(server);
        return server;
    }

    public HttpServer createDefault(int port) throws IOException {
        return create(port, DEFAULT_BACKLOG);
    }

    public HttpServer createWithThreadPool(int port, int threadPoolSize) throws IOException {
        HttpServer server = createDefault(port);
        server.setExecutor(Executors.newFixedThreadPool(threadPoolSize));
        return server;
    }

    public HttpServer createWithCachedThreadPool(int port) throws IOException {
        HttpServer server = createDefault(port);
        server.setExecutor(Executors.newCachedThreadPool());
        return server;
    }

    private void configureServer(HttpServer server) {
        if (server.getExecutor() == null) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
            executor.setCorePoolSize(DEFAULT_THREAD_POOL_SIZE);
            executor.setMaximumPoolSize(DEFAULT_THREAD_POOL_SIZE * 2);
            server.setExecutor(executor);
        }
    }
}
