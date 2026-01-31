package com.httye.userinfoapi;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 聊天记录管理器
 * 监听玩家聊天事件并记录聊天内容
 */
public class ChatRecordManager implements Listener {

    private final UserInfoAPIPlugin plugin;
    private final Logger logger;
    private final Map<String, List<ChatMessage>> chatRecords;
    private final SimpleDateFormat dateFormat;
    private boolean enabled;
    private int maxRecordsPerPlayer;
    private boolean logToFile;

    public ChatRecordManager(UserInfoAPIPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.chatRecords = new ConcurrentHashMap<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.enabled = true;
        this.maxRecordsPerPlayer = 100;
        this.logToFile = false;

        loadConfig();
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("chat-logging.enabled", true);
        this.maxRecordsPerPlayer = config.getInt("chat-logging.max-records-per-player", 100);
        this.logToFile = config.getBoolean("chat-logging.log-to-file", false);

        logger.info("聊天记录功能: " + (enabled ? "启用" : "禁用"));
        logger.info("每个玩家最大记录数: " + maxRecordsPerPlayer);
    }

    /**
     * 重载配置
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * 监听玩家聊天事件
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) {
            return;
        }

        String playerName = event.getPlayer().getName();
        String message = event.getMessage();
        String timestamp = dateFormat.format(new Date());

        ChatMessage chatMessage = new ChatMessage(playerName, message, timestamp);

        // 记录到内存
        addChatRecord(playerName, chatMessage);

        // 记录到文件（如果启用）
        if (logToFile) {
            logToFile(playerName, message, timestamp);
        }

        logger.info("聊天记录 [" + playerName + "]: " + message);
    }

    /**
     * 添加聊天记录
     */
    private void addChatRecord(String playerName, ChatMessage chatMessage) {
        chatRecords.computeIfAbsent(playerName, k -> new ArrayList<>()).add(chatMessage);

        // 限制记录数量
        List<ChatMessage> records = chatRecords.get(playerName);
        if (records.size() > maxRecordsPerPlayer) {
            records.remove(0);
        }
    }

    /**
     * 获取玩家的聊天记录
     */
    public List<ChatMessage> getChatRecords(String playerName) {
        return chatRecords.getOrDefault(playerName, new ArrayList<>());
    }

    /**
     * 获取玩家的聊天记录（限制数量）
     */
    public List<ChatMessage> getChatRecords(String playerName, int limit) {
        List<ChatMessage> records = chatRecords.getOrDefault(playerName, new ArrayList<>());

        if (limit <= 0 || limit >= records.size()) {
            return new ArrayList<>(records);
        }

        // 返回最近N条记录
        return new ArrayList<>(records.subList(records.size() - limit, records.size()));
    }

    /**
     * 获取所有玩家的聊天记录（按玩家分组）
     */
    public Map<String, List<ChatMessage>> getAllChatRecordsByPlayer() {
        return new HashMap<>(chatRecords);
    }

    /**
     * 获取所有玩家的聊天记录（合并列表）
     */
    public List<ChatMessage> getAllChatRecords() {
        List<ChatMessage> allMessages = new ArrayList<>();
        for (List<ChatMessage> messages : chatRecords.values()) {
            allMessages.addAll(messages);
        }
        return allMessages;
    }

    /**
     * 获取所有玩家的聊天记录（合并列表，限制条数）
     * 返回最近N条记录
     */
    public List<ChatMessage> getAllChatRecords(int limit) {
        List<ChatMessage> allMessages = getAllChatRecords();

        if (limit <= 0 || limit >= allMessages.size()) {
            return allMessages;
        }

        // 返回最近N条记录
        return new ArrayList<>(allMessages.subList(allMessages.size() - limit, allMessages.size()));
    }

    /**
     * 清除玩家的聊天记录
     */
    public void clearChatRecords(String playerName) {
        chatRecords.remove(playerName);
        logger.info("已清除玩家 " + playerName + " 的聊天记录");
    }

    /**
     * 清除所有聊天记录
     */
    public void clearAllChatRecords() {
        int totalRecords = getTotalRecordCount();
        chatRecords.clear();
        logger.info("已清除所有聊天记录，共 " + totalRecords + " 条");
    }

    /**
     * 获取总记录数
     */
    public int getTotalRecordCount() {
        int total = 0;
        for (List<ChatMessage> records : chatRecords.values()) {
            total += records.size();
        }
        return total;
    }

    /**
     * 获取有聊天记录的玩家数量
     */
    public int getPlayerCount() {
        return chatRecords.size();
    }

    /**
     * 记录到文件
     */
    private void logToFile(String playerName, String message, String timestamp) {
        // 简单实现：写入日志文件
        logger.info("[CHAT LOG] " + timestamp + " [" + playerName + "] " + message);
        // 实际项目中可以写入专门的日志文件
    }

    /**
     * 聊天消息类
     */
    public static class ChatMessage {
        private final String playerName;
        private final String message;
        private final String timestamp;

        public ChatMessage(String playerName, String message, String timestamp) {
            this.playerName = playerName;
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getMessage() {
            return message;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("playerName", playerName);
            map.put("message", message);
            map.put("timestamp", timestamp);
            return map;
        }
    }
}