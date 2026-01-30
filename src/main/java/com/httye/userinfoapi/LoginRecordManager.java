package com.httye.userinfoapi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LoginRecordManager implements Listener {
    
    private final UserInfoAPIPlugin plugin;
    private final Gson gson;
    private final Map<UUID, LoginSession> activeSessions;
    private final File dataFolder;
    private final SimpleDateFormat dateFormat;
    
    public LoginRecordManager(UserInfoAPIPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.activeSessions = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "login_records");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        LoginSession session = new LoginSession(
            player.getName(),
            playerId.toString(),
            event.getAddress().getHostAddress(),
            new Date(),
            null,
            0
        );
        
        activeSessions.put(playerId, session);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        LoginSession session = activeSessions.remove(playerId);
        if (session != null) {
            session.logoutTime = new Date();
            session.onlineTime = (session.logoutTime.getTime() - session.loginTime.getTime()) / 1000; // 秒
            
            saveLoginRecord(session);
        }
    }
    
    private void saveLoginRecord(LoginSession session) {
        try {
            String fileName = session.playerId + "_" + formatDateFile(session.loginTime) + ".json";
            File recordFile = new File(dataFolder, fileName);
            
            JsonObject record = new JsonObject();
            record.addProperty("username", session.username);
            record.addProperty("playerId", session.playerId);
            record.addProperty("ipAddress", session.ipAddress);
            record.addProperty("loginTime", formatDate(session.loginTime));
            record.addProperty("logoutTime", formatDate(session.logoutTime));
            record.addProperty("onlineTime", session.onlineTime);
            
            try (FileWriter writer = new FileWriter(recordFile)) {
                gson.toJson(record, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("保存登录记录失败: " + e.getMessage());
        }
    }
    
    public JsonObject getLoginRecords(String username, int limit) {
        JsonObject result = new JsonObject();
        JsonArray records = new JsonArray();
        
        try {
            Player player = plugin.getServer().getPlayer(username);
            if (player == null) {
                result.addProperty("error", "玩家未找到");
                return result;
            }
            
            String playerId = player.getUniqueId().toString();
            File[] recordFiles = dataFolder.listFiles((dir, name) -> name.startsWith(playerId));
            
            if (recordFiles != null) {
                // 按时间排序，最新的在前
                Arrays.sort(recordFiles, (a, b) -> b.getName().compareTo(a.getName()));
                
                int count = 0;
                for (File file : recordFiles) {
                    if (count >= limit) break;
                    
                    try (FileReader reader = new FileReader(file)) {
                        JsonObject record = gson.fromJson(reader, JsonObject.class);
                        records.add(record);
                        count++;
                    } catch (IOException e) {
                        plugin.getLogger().warning("读取登录记录失败: " + e.getMessage());
                    }
                }
            }
            
            // 添加当前会话（如果在线）
            LoginSession currentSession = activeSessions.get(player.getUniqueId());
            if (currentSession != null) {
                JsonObject current = new JsonObject();
                current.addProperty("username", currentSession.username);
                current.addProperty("playerId", currentSession.playerId);
                current.addProperty("ipAddress", currentSession.ipAddress);
                current.addProperty("loginTime", formatDate(currentSession.loginTime));
                current.addProperty("logoutTime", "在线中");
                current.addProperty("onlineTime", (new Date().getTime() - currentSession.loginTime.getTime()) / 1000);
                current.addProperty("isOnline", true);
                // 创建新的JsonArray，将当前会话放在最前面，然后添加其他记录
                JsonArray newRecords = new JsonArray();
                newRecords.add(current);
                for (int i = 0; i < records.size(); i++) {
                    newRecords.add(records.get(i));
                }
                records = newRecords;
            }
            
            result.addProperty("username", username);
            result.add("records", records);
            result.addProperty("totalRecords", records.size());
            
        } catch (Exception e) {
            result.addProperty("error", "获取登录记录失败: " + e.getMessage());
        }
        
        return result;
    }
    
    public JsonObject getCurrentOnlinePlayers() {
        JsonObject result = new JsonObject();
        JsonArray players = new JsonArray();
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            JsonObject playerInfo = new JsonObject();
            playerInfo.addProperty("username", player.getName());
            playerInfo.addProperty("uuid", player.getUniqueId().toString());
            playerInfo.addProperty("displayName", player.getDisplayName());
            
            LoginSession session = activeSessions.get(player.getUniqueId());
            if (session != null) {
                playerInfo.addProperty("ipAddress", session.ipAddress);
                playerInfo.addProperty("loginTime", formatDate(session.loginTime));
                playerInfo.addProperty("onlineTime", (new Date().getTime() - session.loginTime.getTime()) / 1000);
            }
            
            // 添加总在线时长
            long totalOnlineTime = getTotalOnlineTime(player.getName());
            playerInfo.addProperty("totalOnlineTime", totalOnlineTime);
            
            players.add(playerInfo);
        }
        
        result.addProperty("count", players.size());
        result.add("players", players);
        return result;
    }
    
    /**
     * 获取玩家的总在线时长（秒）
     */
    public long getTotalOnlineTime(String username) {
        long totalSeconds = 0;
        
        try {
            Player player = plugin.getServer().getPlayer(username);
            if (player != null) {
                String playerId = player.getUniqueId().toString();
                File[] recordFiles = dataFolder.listFiles((dir, name) -> name.startsWith(playerId));
                
                if (recordFiles != null) {
                    for (File file : recordFiles) {
                        try (FileReader reader = new FileReader(file)) {
                            JsonObject record = gson.fromJson(reader, JsonObject.class);
                            if (record.has("onlineTime")) {
                                totalSeconds += record.get("onlineTime").getAsLong();
                            }
                        } catch (IOException e) {
                            plugin.getLogger().warning("读取登录记录失败: " + e.getMessage());
                        }
                    }
                }
            }
            
            // 添加当前会话的在线时间（如果在线）
            if (player != null) {
                LoginSession currentSession = activeSessions.get(player.getUniqueId());
                if (currentSession != null) {
                    totalSeconds += (new Date().getTime() - currentSession.loginTime.getTime()) / 1000;
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("计算总在线时长失败: " + e.getMessage());
        }
        
        return totalSeconds;
    }
    
    /**
     * 获取玩家当前会话的在线时长（秒）
     */
    public long getCurrentSessionOnlineTime(String username) {
        Player player = plugin.getServer().getPlayer(username);
        if (player != null) {
            LoginSession currentSession = activeSessions.get(player.getUniqueId());
            if (currentSession != null) {
                return (new Date().getTime() - currentSession.loginTime.getTime()) / 1000;
            }
        }
        return 0;
    }
    
    private String formatDate(Date date) {
        return dateFormat.format(date);
    }
    
    private String formatDateFile(Date date) {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
    }
    
    private static class LoginSession {
        final String username;
        final String playerId;
        final String ipAddress;
        final Date loginTime;
        Date logoutTime;
        long onlineTime;
        
        LoginSession(String username, String playerId, String ipAddress, Date loginTime, Date logoutTime, long onlineTime) {
            this.username = username;
            this.playerId = playerId;
            this.ipAddress = ipAddress;
            this.loginTime = loginTime;
            this.logoutTime = logoutTime;
            this.onlineTime = onlineTime;
        }
    }
}