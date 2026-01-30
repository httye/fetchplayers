package com.example.userinfoapi;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MiningStatsHandler implements HttpHandler {
    
    private final UserInfoAPIPlugin plugin;
    private final MiningStatsManager miningStatsManager;
    
    public MiningStatsHandler(UserInfoAPIPlugin plugin) {
        this.plugin = plugin;
        this.miningStatsManager = plugin.getMiningStatsManager();
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            
            // 检查API密钥
            String apiKey = extractApiKey(exchange);
            if (apiKey != null && !apiKey.isEmpty()) {
                if (!plugin.getSecurityManager().validateApiKey(apiKey)) {
                    sendResponse(exchange, 401, createError("API密钥无效"));
                    return;
                }
                plugin.getSecurityManager().recordApiKeyUsage(apiKey);
            } else if (plugin.getSecurityManager().isSecurityEnabled()) {
                sendResponse(exchange, 401, createError("需要API密钥"));
                return;
            }
            
            // 检查API限流
            if (plugin.getRateLimitHandler() != null) {
                // 这里需要通过APIServer来处理限流，因为RateLimitHandler是包装器
                // 我们直接记录请求到RateLimitHandler
                String clientId = getClientIdentifier(exchange);
                if (plugin.getRateLimitHandler().getRateLimitStats(clientId).isMinuteLimitReached() ||
                    plugin.getRateLimitHandler().getRateLimitStats(clientId).isHourLimitReached()) {
                    sendResponse(exchange, 429, createError("请求频率过高，请稍后再试"));
                    return;
                }
            }
            
            if (method.equals("GET")) {
                handleGetRequest(exchange, path, query);
            } else {
                sendResponse(exchange, 405, createError("方法不支持"));
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("处理挖掘统计请求时出错: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, 500, createError("服务器内部错误"));
        }
    }
    
    private void handleGetRequest(HttpExchange exchange, String path, String query) throws IOException {
        String[] pathParts = path.split("/");
        
        if (pathParts.length >= 4) {
            String action = pathParts[3];
            
            switch (action) {
                case "player":
                    handlePlayerStats(exchange, query);
                    break;
                case "leaderboard":
                    handleLeaderboard(exchange, query);
                    break;
                case "summary":
                    handlePlayerSummary(exchange, query);
                    break;
                case "all":
                    handleAllStats(exchange);
                    break;
                default:
                    sendResponse(exchange, 404, createError("未知的挖掘统计端点"));
            }
        } else {
            sendResponse(exchange, 400, createError("请求路径格式错误"));
        }
    }
    
    private void handlePlayerStats(HttpExchange exchange, String query) throws IOException {
        String username = getQueryParam(query, "username");
        
        if (username == null || username.trim().isEmpty()) {
            sendResponse(exchange, 400, createError("用户名参数缺失"));
            return;
        }
        
        JsonObject result = miningStatsManager.getPlayerMiningStats(username);
        sendResponse(exchange, 200, result);
    }
    
    private void handleLeaderboard(HttpExchange exchange, String query) throws IOException {
        String blockType = getQueryParam(query, "block");
        String limitParam = getQueryParam(query, "limit");
        
        int limit = 10; // 默认显示前10名
        if (limitParam != null) {
            try {
                limit = Math.min(Math.max(Integer.parseInt(limitParam), 1), 100); // 限制在1-100之间
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }
        
        if (blockType == null || blockType.trim().isEmpty()) {
            // 获取所有挖掘排行榜
            JsonObject result = miningStatsManager.getAllMiningLeaderboards(limit);
            sendResponse(exchange, 200, result);
        } else {
            // 获取指定方块的排行榜
            JsonObject result = miningStatsManager.getMiningLeaderboard(blockType, limit);
            sendResponse(exchange, 200, result);
        }
    }
    
    private void handlePlayerSummary(HttpExchange exchange, String query) throws IOException {
        String username = getQueryParam(query, "username");
        
        if (username == null || username.trim().isEmpty()) {
            sendResponse(exchange, 400, createError("用户名参数缺失"));
            return;
        }
        
        JsonObject result = miningStatsManager.getPlayerMiningSummary(username);
        sendResponse(exchange, 200, result);
    }
    
    private void handleAllStats(HttpExchange exchange) throws IOException {
        JsonObject result = miningStatsManager.getAllStats();
        sendResponse(exchange, 200, result);
    }
    
    private String getQueryParam(String query, String paramName) {
        if (query == null) return null;
        
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                return keyValue[1];
            }
        }
        return null;
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, JsonObject response) throws IOException {
        String responseText = plugin.getGson().toJson(response);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, 0);
            return;
        }
        
        byte[] responseBytes = responseText.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    private JsonObject createError(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        return error;
    }
    
    private String extractApiKey(HttpExchange exchange) {
        // 从请求头获取
        String apiKey = exchange.getRequestHeaders().getFirst("X-API-Key");
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }
        
        // 从查询参数获取
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2 && keyValue[0].equals("api_key")) {
                    return keyValue[1];
                }
            }
        }
        
        return null;
    }
    
    private String getClientIdentifier(HttpExchange exchange) {
        // 首先尝试获取API密钥
        String apiKey = extractApiKey(exchange);
        if (apiKey != null && !apiKey.isEmpty()) {
            return "key:" + apiKey;
        }
        
        // 否则使用IP地址
        String ipAddress = exchange.getRemoteAddress().getAddress().getHostAddress();
        return "ip:" + ipAddress;
    }
}