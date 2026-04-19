package com.proxy;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Command(name = "caching-proxy", mixinStandardHelpOptions = true)
public class CachingProxyCLI implements Runnable {

    @Option(names = { "--port" }, defaultValue = "3000")
    private int port;

    @Option(names = { "--origin" })
    private String origin;

    @Option(names = { "--clear-cache" })
    private boolean clearCache;

    @Override
    public void run() {
        Jedis jedis = new Jedis("localhost", 6379);

        if (clearCache) {
            jedis.flushAll();
            System.out.println("Cache cleared successfully!");
            jedis.close();
            return;
        }

        if (origin == null) {
            System.out.println("Error: --origin is required. Example: --origin https://dummyjson.com");
            return;
        }

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new ProxyHandler(origin, jedis));
            server.setExecutor(null);

            System.out.println("Caching Proxy started on port: " + port);
            System.out.println("Forwarding to: " + origin);
            server.start();
        } catch (IOException e) {
            System.err.println("Server failed: " + e.getMessage());
        }
    }

    static class ProxyHandler implements HttpHandler {
        private final String origin;
        private final Jedis jedis;
        private final HttpClient httpClient = HttpClient.newHttpClient();

        public ProxyHandler(String origin, Jedis jedis) {
            this.origin = origin;
            this.jedis = jedis;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String cacheKey = "proxy:" + path;

            // 1. Önce Redis'e Bak
            String cachedData = jedis.get(cacheKey);

            if (cachedData != null) {
                System.out.println("CACHE HIT: " + path);
                sendResponse(exchange, cachedData.getBytes(), "HIT");
                return;
            }

            // 2. Redis'te yoksa Origin'e Git (MISS)
            System.out.println("CACHE MISS: " + path);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(origin + path))
                        .build();

                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                byte[] body = response.body();

                // 3. Redis'e Kaydet (Cache it!)
                jedis.setex(cacheKey, 3600, new String(body));

                sendResponse(exchange, body, "MISS");
            } catch (Exception e) {
                sendResponse(exchange, "Error fetching from origin".getBytes(), "ERROR");
            }
        }

        private void sendResponse(HttpExchange exchange, byte[] body, String cacheStatus) throws IOException {
            exchange.getResponseHeaders().set("X-Cache", cacheStatus);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    public static void main(String[] args) {
        new CommandLine(new CachingProxyCLI()).execute(args);
    }
}