package com.httye.userinfoapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * 数据导出处理器
 * 支持导出玩家数据到CSV和JSON格式
 */
public class DataExportHandler implements HttpHandler {
    
    private final UserInfoAPIPlugin plugin;
    
    public DataExportHandler(UserInfoAPIPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String format = getQueryParam(query, "format");
        String type = getQueryParam(query, "type");
        String username = getQueryParam(query, "username");
        
        if (format == null) {
            format = "json"; // 默认格式
        }
        if (type == null) {
            type = "players"; // 默认类型
        }
        
        try {
            String content = "";
            String contentType = "";
            String filename = "";
            
            switch (type.toLowerCase()) {
                case "players":
                    if ("csv".equalsIgnoreCase(format)) {
                        content = exportPlayersToCSV();
                        contentType = "text/csv";
                        filename = "players_" + getCurrentTimestamp() + ".csv";
                    } else {
                        content = exportPlayersToJSON();
                        contentType = "application/json";
                        filename = "players_" + getCurrentTimestamp() + ".json";
                    }
                    break;
                    
                case "login-records":
                    if (username == null) {
                        sendErrorResponse(exchange, 400, "导出登录记录需要指定username参数");
                        return;
                    }
                    if ("csv".equalsIgnoreCase(format)) {
                        content = exportLoginRecordsToCSV(username);
                        contentType = "text/csv";
                        filename = "login_records_" + username + "_" + getCurrentTimestamp() + ".csv";
                    } else {
                        content = exportLoginRecordsToJSON(username);
                        contentType = "application/json";
                        filename = "login_records_" + username + "_" + getCurrentTimestamp() + ".json";
                    }
                    break;
                    
                case "online-players":
                    if ("csv".equalsIgnoreCase(format)) {
                        content = exportOnlinePlayersToCSV();
                        contentType = "text/csv";
                        filename = "online_players_" + getCurrentTimestamp() + ".csv";
                    } else {
                        content = exportOnlinePlayersToJSON();
                        contentType = "application/json";
                        filename = "online_players_" + getCurrentTimestamp() + ".json";
                    }
                    break;
                    
                default:
                    sendErrorResponse(exchange, 400, "不支持的导出类型: " + type);
                    return;
            }
            
            // 设置响应头
            exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=UTF-8");
            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            
            byte[] bytes = content.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            
            try (java.io.OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "导出数据时发生错误: " + e.getMessage());
        }
    }
    
    private String exportPlayersToCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("用户名,UUID,等级,经验值,生命值,饥饿值,游戏模式,世界,X坐标,Y坐标,Z坐标,是否在线\n");
        
        // 获取所有在线玩家
        List<org.bukkit.entity.Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        
        UserInfoService service = new UserInfoService();
        
        for (org.bukkit.entity.Player player : onlinePlayers) {
            try {
                JsonObject info = service.getUserInfo(player.getName());
                if (info != null) {
                    csv.append(escapeCSV(info.get("username").getAsString())).append(",");
                    csv.append(escapeCSV(info.get("uuid").getAsString())).append(",");
                    csv.append(info.get("level").getAsInt()).append(",");
                    csv.append(info.get("exp").getAsDouble()).append(",");
                    csv.append(info.get("health").getAsDouble()).append(",");
                    csv.append(info.get("foodLevel").getAsInt()).append(",");
                    csv.append(escapeCSV(info.get("gameMode").getAsString())).append(",");
                    
                    if (info.has("location")) {
                        JsonObject location = info.getAsJsonObject("location");
                        csv.append(escapeCSV(location.get("world").getAsString())).append(",");
                        csv.append(location.get("x").getAsDouble()).append(",");
                        csv.append(location.get("y").getAsDouble()).append(",");
                        csv.append(location.get("z").getAsDouble()).append(",");
                    } else {
                        csv.append(",,,,");
                    }
                    
                    csv.append("是\n");
                }
            } catch (Exception e) {
                // 跳过出错的玩家
            }
        }
        
        return csv.toString();
    }
    
    private String exportPlayersToJSON() {
        JsonObject result = new JsonObject();
        result.addProperty("exportTime", getCurrentTimestamp());
        result.addProperty("type", "players");
        
        JsonArray players = new JsonArray();
        UserInfoService service = new UserInfoService();
        
        // 获取所有在线玩家
        List<org.bukkit.entity.Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        
        for (org.bukkit.entity.Player player : onlinePlayers) {
            try {
                JsonObject info = service.getUserInfo(player.getName());
                if (info != null) {
                    players.add(info);
                }
            } catch (Exception e) {
                // 跳过出错的玩家
            }
        }
        
        result.add("players", players);
        result.addProperty("count", players.size());
        
        return result.toString();
    }
    
    private String exportLoginRecordsToCSV(String username) {
        StringBuilder csv = new StringBuilder();
        csv.append("用户名,登录时间,登出时间,在线时长(秒),IP地址,是否在线\n");
        
        try {
            JsonObject records = plugin.getLoginRecordManager().getLoginRecords(username, 100);
            JsonArray recordArray = records.getAsJsonArray("records");
            
            for (int i = 0; i < recordArray.size(); i++) {
                JsonObject record = recordArray.get(i).getAsJsonObject();
                
                csv.append(escapeCSV(record.get("username").getAsString())).append(",");
                csv.append(escapeCSV(record.get("loginTime").getAsString())).append(",");
                csv.append(escapeCSV(record.get("logoutTime").getAsString())).append(",");
                csv.append(record.get("onlineTime").getAsInt()).append(",");
                csv.append(escapeCSV(record.get("ipAddress").getAsString())).append(",");
                csv.append(record.get("isOnline").getAsBoolean() ? "是" : "否").append("\n");
            }
        } catch (Exception e) {
            csv.append("导出失败: ").append(e.getMessage()).append("\n");
        }
        
        return csv.toString();
    }
    
    private String exportLoginRecordsToJSON(String username) {
        try {
            int maxRecords = plugin.getConfig().getInt("data-export.max-records", 100);
            JsonObject records = plugin.getLoginRecordManager().getLoginRecords(username, maxRecords);
            records.addProperty("exportTime", getCurrentTimestamp());
            records.addProperty("exportedUsername", username);
            return records.toString();
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "导出失败: " + e.getMessage());
            return error.toString();
        }
    }
    
    private String exportOnlinePlayersToCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("用户名,UUID,显示名称,IP地址,登录时间,在线时长(秒)\n");
        
        try {
            JsonObject onlineData = getOnlinePlayersData();
            JsonArray players = onlineData.getAsJsonArray("players");
            
            for (int i = 0; i < players.size(); i++) {
                JsonObject player = players.get(i).getAsJsonObject();
                
                csv.append(escapeCSV(player.get("username").getAsString())).append(",");
                csv.append(escapeCSV(player.get("uuid").getAsString())).append(",");
                csv.append(escapeCSV(player.get("displayName").getAsString())).append(",");
                csv.append(escapeCSV(player.get("ipAddress").getAsString())).append(",");
                csv.append(escapeCSV(player.get("loginTime").getAsString())).append(",");
                csv.append(player.get("onlineTime").getAsInt()).append("\n");
            }
        } catch (Exception e) {
            csv.append("导出失败: ").append(e.getMessage()).append("\n");
        }
        
        return csv.toString();
    }
    
    private String exportOnlinePlayersToJSON() {
        try {
            JsonObject onlineData = getOnlinePlayersData();
            onlineData.addProperty("exportTime", getCurrentTimestamp());
            return onlineData.toString();
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "导出失败: " + e.getMessage());
            return error.toString();
        }
    }
    
    private JsonObject getOnlinePlayersData() {
        JsonObject result = new JsonObject();
        JsonArray players = new JsonArray();
        
        List<org.bukkit.entity.Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        
        for (org.bukkit.entity.Player player : onlinePlayers) {
            try {
                JsonObject playerInfo = new JsonObject();
                playerInfo.addProperty("username", player.getName());
                playerInfo.addProperty("uuid", player.getUniqueId().toString());
                playerInfo.addProperty("displayName", player.getDisplayName());
                playerInfo.addProperty("ipAddress", player.getAddress().getAddress().getHostAddress());
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                playerInfo.addProperty("loginTime", sdf.format(new Date()));
                
                // 这里可以添加更详细的在线时长计算
                playerInfo.addProperty("onlineTime", 0);
                
                players.add(playerInfo);
            } catch (Exception e) {
                // 跳过出错的玩家
            }
        }
        
        result.add("players", players);
        result.addProperty("count", players.size());
        
        return result;
    }
    
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return sdf.format(new Date());
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
    
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        
        byte[] bytes = error.toString().getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}