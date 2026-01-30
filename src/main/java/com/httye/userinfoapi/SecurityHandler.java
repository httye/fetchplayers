package com.httye.userinfoapi;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class SecurityHandler implements HttpHandler {
    
    private final HttpHandler wrappedHandler;
    private final UserInfoAPIPlugin plugin;
    
    public SecurityHandler(HttpHandler wrappedHandler) {
        this.wrappedHandler = wrappedHandler;
        this.plugin = UserInfoAPIPlugin.getInstance();
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 处理 OPTIONS 预检请求
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            sendOptionsResponse(exchange);
            return;
        }
        
        // 安全检查
        if (!checkSecurity(exchange)) {
            return;
        }
        
        // 记录API密钥使用
        String apiKey = getApiKey(exchange);
        if (apiKey != null) {
            plugin.getSecurityManager().recordApiKeyUsage(apiKey);
        }
        
        // 调用实际的处理器
        wrappedHandler.handle(exchange);
    }
    
    private boolean checkSecurity(HttpExchange exchange) throws IOException {
        // IP地址验证
        String clientIP = getClientIP(exchange);
        if (!plugin.getSecurityManager().validateIPAddress(clientIP)) {
            sendSecurityError(exchange, 403, "IP地址被拒绝访问");
            return false;
        }
        
        // API密钥验证
        String apiKey = getApiKey(exchange);
        if (!plugin.getSecurityManager().validateApiKey(apiKey)) {
            sendSecurityError(exchange, 401, "无效的API密钥或缺少认证");
            return false;
        }
        
        return true;
    }
    
    private String getClientIP(HttpExchange exchange) {
        // 获取客户端真实IP地址
        InetSocketAddress remoteAddress = exchange.getRemoteAddress();
        return remoteAddress.getAddress().getHostAddress();
    }
    
    private String getApiKey(HttpExchange exchange) {
        // 从请求头获取API密钥
        String apiKey = exchange.getRequestHeaders().getFirst("X-API-Key");
        if (apiKey == null) {
            // 从查询参数获取API密钥
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && keyValue[0].equals("api_key")) {
                        return keyValue[1];
                    }
                }
            }
        }
        return apiKey;
    }
    
    private void sendSecurityError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String response = "{\"error\": \"" + message + "\", \"code\": " + statusCode + "}";
        
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        
        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private void sendOptionsResponse(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
        exchange.getResponseHeaders().add("Access-Control-Max-Age", "86400");
        
        exchange.sendResponseHeaders(200, -1);
    }
}