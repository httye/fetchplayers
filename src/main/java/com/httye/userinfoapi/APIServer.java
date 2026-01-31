package com.httye.userinfoapi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

public class APIServer {
    
    private HttpServer server;
    private final String host;
    private final int port;
    private final Gson gson;
    
    private final UserInfoAPIPlugin plugin;
    private final RateLimitHandler rateLimitHandler;
    
    // 统计信息
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final List<Long> responseTimes = new ArrayList<>();
    
    public APIServer(UserInfoAPIPlugin plugin, String host, int port) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.gson = new Gson();
        this.rateLimitHandler = new RateLimitHandler(null, plugin);
    }
    
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        
        // 注册API路由 - 使用限流处理器包装
        server.createContext("/api/user/info", new RateLimitHandler(new SecurityHandler(new UserInfoHandler()), plugin));
        server.createContext("/api/user/level", new RateLimitHandler(new SecurityHandler(new UserLevelHandler()), plugin));
        server.createContext("/api/user/location", new RateLimitHandler(new SecurityHandler(new UserLocationHandler()), plugin));
        server.createContext("/api/user/inventory", new RateLimitHandler(new SecurityHandler(new UserInventoryHandler()), plugin));
        server.createContext("/api/user/login-records", new RateLimitHandler(new SecurityHandler(new LoginRecordsHandler()), plugin));
        server.createContext("/api/online-players", new RateLimitHandler(new SecurityHandler(new OnlinePlayersHandler()), plugin));
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/security/info", new RateLimitHandler(new SecurityHandler(new SecurityInfoHandler()), plugin));
        
        // 新增功能路由
        server.createContext("/api/user/batch", new RateLimitHandler(new SecurityHandler(new BatchUserHandler(plugin)), plugin));
        server.createContext("/api/export", new RateLimitHandler(new SecurityHandler(new DataExportHandler(plugin)), plugin));

        // 聊天记录和服务器资源监控路由
        server.createContext("/api/chat-records", new RateLimitHandler(new SecurityHandler(new ChatRecordsHandler()), plugin));
        server.createContext("/api/server/resources", new RateLimitHandler(new SecurityHandler(new ServerResourceHandler()), plugin));
        
        // 设置线程池
        int threadPoolSize = plugin.getConfig().getInt("advanced.thread-pool-size", 10);
        server.setExecutor(Executors.newFixedThreadPool(threadPoolSize));
        
        server.start();
    }
    
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
        if (rateLimitHandler != null) {
            rateLimitHandler.shutdown();
        }
    }
    
    // 统计方法
    public void recordRequest(boolean success, long responseTime) {
        totalRequests.incrementAndGet();
        if (success) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        
        synchronized (responseTimes) {
            responseTimes.add(responseTime);
            // 只保留最近1000个响应时间
            if (responseTimes.size() > 1000) {
                responseTimes.remove(0);
            }
        }
    }
    
    public int getTotalRequests() {
        return totalRequests.get();
    }
    
    public int getSuccessfulRequests() {
        return successfulRequests.get();
    }
    
    public int getFailedRequests() {
        return failedRequests.get();
    }
    
    public int getActiveConnections() {
        return 0; // 这里可以添加更复杂的连接数统计
    }
    
    public double getAverageResponseTime() {
        synchronized (responseTimes) {
            if (responseTimes.isEmpty()) {
                return 0.0;
            }
            long sum = 0;
            for (Long time : responseTimes) {
                sum += time;
            }
            return (double) sum / responseTimes.size();
        }
    }
    
    public RateLimitHandler getRateLimitHandler() {
        return rateLimitHandler;
    }
    
    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            JsonObject response = new JsonObject();
            response.addProperty("status", "online");
            response.addProperty("plugin", "UserInfoAPI");
            response.addProperty("version", "1.0.0");
            
            sendResponse(exchange, 200, response.toString());
        }
    }
    
    private class UserInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String username = getQueryParam(query, "username");
            
            if (username == null || username.isEmpty()) {
                sendErrorResponse(exchange, 400, "缺少用户名参数");
                return;
            }
            
            UserInfoService service = new UserInfoService();
            JsonObject userInfo = service.getUserInfo(username);
            
            if (userInfo == null) {
                sendErrorResponse(exchange, 404, "用户未找到");
                return;
            }
            
            sendResponse(exchange, 200, userInfo.toString());
        }
    }
    
    private class UserLevelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String username = getQueryParam(query, "username");
            
            if (username == null || username.isEmpty()) {
                sendErrorResponse(exchange, 400, "缺少用户名参数");
                return;
            }
            
            UserInfoService service = new UserInfoService();
            JsonObject levelInfo = service.getUserLevel(username);
            
            if (levelInfo == null) {
                sendErrorResponse(exchange, 404, "用户未找到");
                return;
            }
            
            sendResponse(exchange, 200, levelInfo.toString());
        }
    }
    
    private class UserLocationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String username = getQueryParam(query, "username");
            
            if (username == null || username.isEmpty()) {
                sendErrorResponse(exchange, 400, "缺少用户名参数");
                return;
            }
            
            UserInfoService service = new UserInfoService();
            JsonObject locationInfo = service.getUserLocation(username);
            
            if (locationInfo == null) {
                sendErrorResponse(exchange, 404, "用户未找到");
                return;
            }
            
            sendResponse(exchange, 200, locationInfo.toString());
        }
    }
    
    private class UserInventoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String username = getQueryParam(query, "username");
            
            if (username == null || username.isEmpty()) {
                sendErrorResponse(exchange, 400, "缺少用户名参数");
                return;
            }
            
            UserInfoService service = new UserInfoService();
            JsonObject inventoryInfo = service.getUserInventory(username);
            
            if (inventoryInfo == null) {
                sendErrorResponse(exchange, 404, "用户未找到");
                return;
            }
            
            sendResponse(exchange, 200, inventoryInfo.toString());
        }
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
        
        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        sendResponse(exchange, statusCode, error.toString());
    }
    
    private String getQueryParam(String query, String paramName) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                return keyValue[1];
            }
        }
        return null;
    }
}