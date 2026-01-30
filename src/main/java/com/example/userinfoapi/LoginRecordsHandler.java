package com.example.userinfoapi;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class LoginRecordsHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String username = getQueryParam(query, "username");
        String limitStr = getQueryParam(query, "limit");
        
        int limit = 10; // 默认限制10条记录
        if (limitStr != null) {
            try {
                limit = Integer.parseInt(limitStr);
                if (limit > 100) limit = 100; // 最大100条
                if (limit < 1) limit = 1;
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }
        
        if (username == null || username.isEmpty()) {
            sendErrorResponse(exchange, 400, "缺少用户名参数");
            return;
        }
        
        UserInfoAPIPlugin plugin = UserInfoAPIPlugin.getInstance();
        JsonObject loginRecords = plugin.getLoginRecordManager().getLoginRecords(username, limit);
        
        if (loginRecords.has("error")) {
            sendErrorResponse(exchange, 404, loginRecords.get("error").getAsString());
            return;
        }
        
        sendResponse(exchange, 200, loginRecords.toString());
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        
        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("code", statusCode);
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