package com.example.userinfoapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量用户查询处理器
 * 支持一次性查询多个玩家的信息
 */
public class BatchUserHandler implements HttpHandler {
    
    private final UserInfoAPIPlugin plugin;
    
    public BatchUserHandler(UserInfoAPIPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendErrorResponse(exchange, 405, "只支持POST方法");
            return;
        }
        
        try {
            // 读取请求体
            InputStream is = exchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            String requestBody = sb.toString();
            if (requestBody.isEmpty()) {
                sendErrorResponse(exchange, 400, "请求体不能为空");
                return;
            }
            
            // 解析JSON请求
            com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
            JsonObject request = parser.parse(requestBody).getAsJsonObject();
            
            if (!request.has("usernames") || !request.get("usernames").isJsonArray()) {
                sendErrorResponse(exchange, 400, "请求必须包含usernames数组");
                return;
            }
            
            JsonArray usernamesArray = request.getAsJsonArray("usernames");
            if (usernamesArray.size() > 50) {
                sendErrorResponse(exchange, 400, "一次最多查询50个玩家");
                return;
            }
            
            // 获取查询类型
            String queryType = request.has("queryType") ? request.get("queryType").getAsString() : "info";
            
            JsonObject response = new JsonObject();
            JsonArray results = new JsonArray();
            
            UserInfoService service = new UserInfoService();
            
            // 批量查询
            for (int i = 0; i < usernamesArray.size(); i++) {
                String username = usernamesArray.get(i).getAsString();
                JsonObject playerResult = new JsonObject();
                playerResult.addProperty("username", username);
                
                try {
                    JsonObject data = null;
                    switch (queryType.toLowerCase()) {
                        case "info":
                            data = service.getUserInfo(username);
                            break;
                        case "level":
                            data = service.getUserLevel(username);
                            break;
                        case "location":
                            data = service.getUserLocation(username);
                            break;
                        case "inventory":
                            data = service.getUserInventory(username);
                            break;
                        default:
                            playerResult.addProperty("error", "不支持的查询类型: " + queryType);
                            results.add(playerResult);
                            continue;
                    }
                    
                    // 检查是否找到了玩家（在线或离线）
                    if (data != null) {
                        playerResult.add("data", data);
                        playerResult.addProperty("success", true);
                    } else {
                        playerResult.addProperty("error", "玩家未找到（可能从未加入过服务器）");
                        playerResult.addProperty("success", false);
                    }
                    
                    if (data != null) {
                        playerResult.add("data", data);
                        playerResult.addProperty("success", true);
                    } else {
                        playerResult.addProperty("error", "玩家未找到或不在线");
                        playerResult.addProperty("success", false);
                    }
                } catch (Exception e) {
                    playerResult.addProperty("error", "查询失败: " + e.getMessage());
                    playerResult.addProperty("success", false);
                }
                
                results.add(playerResult);
            }
            
            response.add("results", results);
            response.addProperty("total", results.size());
            response.addProperty("queryType", queryType);
            
            sendResponse(exchange, 200, response.toString());
            
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "处理请求时发生错误: " + e.getMessage());
        }
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        
        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        sendResponse(exchange, statusCode, error.toString());
    }
}