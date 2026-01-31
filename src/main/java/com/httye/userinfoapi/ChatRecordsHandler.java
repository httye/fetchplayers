package com.httye.userinfoapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 聊天记录API处理器
 */
public class ChatRecordsHandler implements HttpHandler {

    private final UserInfoAPIPlugin plugin;
    private final APIServer apiServer;

    public ChatRecordsHandler() {
        this.plugin = UserInfoAPIPlugin.getInstance();
        this.apiServer = plugin.getApiServer();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long startTime = System.currentTimeMillis();

        try {
            String query = exchange.getRequestURI().getQuery();
            String username = getQueryParam(query, "username");
            String limitStr = getQueryParam(query, "limit");
            String allStr = getQueryParam(query, "all");

            int limit = 0;
            if (limitStr != null && !limitStr.isEmpty()) {
                try {
                    limit = Integer.parseInt(limitStr);
                    if (limit < 0) {
                        sendErrorResponse(exchange, 400, "limit 参数必须大于等于0");
                        return;
                    }
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "limit 参数必须是数字");
                    return;
                }
            }

            boolean getAll = false;
            if (allStr != null && !allStr.isEmpty()) {
                getAll = Boolean.parseBoolean(allStr);
            }

            // 获取聊天记录管理器
            ChatRecordManager chatManager = plugin.getChatRecordManager();
            if (chatManager == null) {
                sendErrorResponse(exchange, 500, "聊天记录管理器未初始化");
                return;
            }

            JsonObject response = new JsonObject();

            if (username != null && !username.isEmpty()) {
                // 获取特定玩家的聊天记录
                List<ChatRecordManager.ChatMessage> records;

                if (limit > 0) {
                    records = chatManager.getChatRecords(username, limit);
                } else {
                    records = chatManager.getChatRecords(username);
                }

                JsonArray messagesArray = new JsonArray();
                for (ChatRecordManager.ChatMessage msg : records) {
                    JsonObject msgObj = new JsonObject();
                    msgObj.addProperty("playerName", msg.getPlayerName());
                    msgObj.addProperty("message", msg.getMessage());
                    msgObj.addProperty("timestamp", msg.getTimestamp());
                    messagesArray.add(msgObj);
                }

                response.addProperty("username", username);
                response.add("messages", messagesArray);
                response.addProperty("count", records.size());

            } else if (getAll) {
                // 获取所有玩家的聊天记录
                List<ChatRecordManager.ChatMessage> allRecords;

                if (limit > 0) {
                    allRecords = chatManager.getAllChatRecords(limit);
                } else {
                    allRecords = chatManager.getAllChatRecords();
                }

                JsonArray messagesArray = new JsonArray();
                for (ChatRecordManager.ChatMessage msg : allRecords) {
                    JsonObject msgObj = new JsonObject();
                    msgObj.addProperty("playerName", msg.getPlayerName());
                    msgObj.addProperty("message", msg.getMessage());
                    msgObj.addProperty("timestamp", msg.getTimestamp());
                    messagesArray.add(msgObj);
                }

                response.add("messages", messagesArray);
                response.addProperty("count", allRecords.size());
                response.addProperty("totalPlayers", chatManager.getPlayerCount());
                response.addProperty("totalMessages", chatManager.getTotalRecordCount());

            } else {
                // 获取所有玩家的聊天记录概览
                response.addProperty("totalPlayers", chatManager.getPlayerCount());
                response.addProperty("totalMessages", chatManager.getTotalRecordCount());
                response.addProperty("description", "使用 all=true 参数获取所有聊天记录");
            }

            long responseTime = System.currentTimeMillis() - startTime;
            response.addProperty("responseTime", responseTime + "ms");

            sendResponse(exchange, 200, response.toString());

            if (apiServer != null) {
                apiServer.recordRequest(true, responseTime);
            }

        } catch (Exception e) {
            e.printStackTrace();

            long responseTime = System.currentTimeMillis() - startTime;

            if (apiServer != null) {
                apiServer.recordRequest(false, responseTime);
            }

            sendErrorResponse(exchange, 500, "处理请求时发生错误: " + e.getMessage());
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