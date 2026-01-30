package com.example.userinfoapi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiningStatsManager implements Listener {
    
    private final UserInfoAPIPlugin plugin;
    private final Gson gson;
    private final Map<UUID, Map<Material, Integer>> playerMiningStats;
    private final File dataFolder;
    
    public MiningStatsManager(UserInfoAPIPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.playerMiningStats = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "mining_stats");
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        loadAllStats();
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        
        Material blockType = event.getBlock().getType();
        
        // 检查是否为可挖掘的方块
        if (isMineableBlock(blockType)) {
            UUID playerId = player.getUniqueId();
            playerMiningStats.putIfAbsent(playerId, new ConcurrentHashMap<>());
            
            Map<Material, Integer> playerStats = playerMiningStats.get(playerId);
            playerStats.put(blockType, playerStats.getOrDefault(blockType, 0) + 1);
            
            // 保存统计数据
            savePlayerStats(playerId);
        }
    }
    
    /**
     * 检查方块是否可挖掘
     */
    private boolean isMineableBlock(Material material) {
        return material.isBlock() && (
            material.name().contains("ORE") ||           // 所有矿石
            material.name().contains("STONE") ||         // 石头类
            material.name().contains("COBBLE") ||        // 圆石
            material.name().contains("DIRT") ||          // 泥土
            material.name().contains("SAND") ||          // 沙子
            material.name().contains("GRAVEL") ||        // 砾石
            material.name().contains("COAL") ||          // 煤炭
            material.name().contains("DIAMOND") ||       // 钻石
            material.name().contains("EMERALD") ||       // 绿宝石
            material.name().contains("GOLD") ||          // 金矿
            material.name().contains("IRON") ||          // 铁矿
            material.name().contains("REDSTONE") ||      // 红石
            material.name().contains("QUARTZ") ||        // 石英
            material.name().contains("NETHER") ||        // 下界相关
            material.name().contains("DEEPSLATE") ||     // 深板岩
            material.name().contains("TUFF") ||          // 凝灰岩
            material.name().contains("CLAY") ||          // 粘土
            material.name().contains("GRAVEL") ||        // 砾石
            material.name().contains("SANDSTONE") ||     // 砂岩
            material.name().contains("STONE") ||         // 各种石头
            material == Material.COAL_ORE ||
            material == Material.IRON_ORE ||
            material == Material.COPPER_ORE ||
            material == Material.GOLD_ORE ||
            material == Material.DIAMOND_ORE ||
            material == Material.EMERALD_ORE ||
            material == Material.REDSTONE_ORE ||
            material == Material.NETHER_QUARTZ_ORE ||
            material == Material.NETHER_GOLD_ORE ||
            material == Material.ANCIENT_DEBRIS ||
            material == Material.DEEPSLATE_COAL_ORE ||
            material == Material.DEEPSLATE_IRON_ORE ||
            material == Material.DEEPSLATE_COPPER_ORE ||
            material == Material.DEEPSLATE_GOLD_ORE ||
            material == Material.DEEPSLATE_DIAMOND_ORE ||
            material == Material.DEEPSLATE_EMERALD_ORE ||
            material == Material.DEEPSLATE_REDSTONE_ORE
        );
    }
    
    /**
     * 获取玩家的挖掘统计
     */
    public JsonObject getPlayerMiningStats(String username) {
        JsonObject result = new JsonObject();
        result.addProperty("username", username);
        
        Player player = plugin.getServer().getPlayer(username);
        if (player == null) {
            result.addProperty("error", "玩家未找到");
            return result;
        }
        
        UUID playerId = player.getUniqueId();
        Map<Material, Integer> stats = playerMiningStats.get(playerId);
        
        if (stats != null) {
            JsonObject blocks = new JsonObject();
            for (Map.Entry<Material, Integer> entry : stats.entrySet()) {
                blocks.addProperty(entry.getKey().name().toLowerCase(), entry.getValue());
            }
            result.add("blocks", blocks);
        } else {
            result.add("blocks", new JsonObject());
        }
        
        return result;
    }
    
    /**
     * 获取挖掘排行榜
     */
    public JsonObject getMiningLeaderboard(String blockType, int limit) {
        JsonObject result = new JsonObject();
        result.addProperty("blockType", blockType);
        result.addProperty("limit", limit);
        
        JsonArray leaderboard = new JsonArray();
        
        // 收集所有玩家的指定方块挖掘数量
        Map<String, Integer> playerBlockCounts = new HashMap<>();
        
        for (Map.Entry<UUID, Map<Material, Integer>> playerEntry : playerMiningStats.entrySet()) {
            String playerName = plugin.getServer().getOfflinePlayer(playerEntry.getKey()).getName();
            if (playerName == null) continue;
            
            Map<Material, Integer> blocks = playerEntry.getValue();
            for (Map.Entry<Material, Integer> blockEntry : blocks.entrySet()) {
                if (blockEntry.getKey().name().equalsIgnoreCase(blockType) || 
                    blockEntry.getKey().name().toLowerCase().contains(blockType.toLowerCase())) {
                    playerBlockCounts.put(playerName, 
                        playerBlockCounts.getOrDefault(playerName, 0) + blockEntry.getValue());
                }
            }
        }
        
        // 按挖掘数量排序
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(playerBlockCounts.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // 创建排行榜
        int rank = 1;
        for (Map.Entry<String, Integer> entry : sortedList) {
            if (rank > limit) break;
            
            JsonObject playerEntry = new JsonObject();
            playerEntry.addProperty("rank", rank);
            playerEntry.addProperty("username", entry.getKey());
            playerEntry.addProperty("count", entry.getValue());
            leaderboard.add(playerEntry);
            
            rank++;
        }
        
        result.add("leaderboard", leaderboard);
        return result;
    }
    
    /**
     * 获取所有挖掘排行榜
     */
    public JsonObject getAllMiningLeaderboards(int limit) {
        JsonObject result = new JsonObject();
        JsonArray leaderboards = new JsonArray();
        
        // 获取所有被挖掘过的方块类型
        Set<Material> allBlockTypes = new HashSet<>();
        for (Map<Material, Integer> playerStats : playerMiningStats.values()) {
            allBlockTypes.addAll(playerStats.keySet());
        }
        
        for (Material blockType : allBlockTypes) {
            JsonObject leaderboard = getMiningLeaderboard(blockType.name(), limit);
            leaderboards.add(leaderboard);
        }
        
        result.add("leaderboards", leaderboards);
        return result;
    }
    
    /**
     * 获取玩家总挖掘量
     */
    public int getTotalMinedBlocks(UUID playerId) {
        Map<Material, Integer> stats = playerMiningStats.get(playerId);
        if (stats == null) return 0;
        
        int total = 0;
        for (int count : stats.values()) {
            total += count;
        }
        return total;
    }
    
    /**
     * 获取玩家挖掘统计摘要
     */
    public JsonObject getPlayerMiningSummary(String username) {
        JsonObject result = new JsonObject();
        result.addProperty("username", username);
        
        Player player = plugin.getServer().getPlayer(username);
        if (player == null) {
            result.addProperty("error", "玩家未找到");
            return result;
        }
        
        UUID playerId = player.getUniqueId();
        Map<Material, Integer> stats = playerMiningStats.get(playerId);
        
        if (stats != null) {
            int totalMined = getTotalMinedBlocks(playerId);
            int uniqueBlocks = stats.size();
            
            result.addProperty("totalMined", totalMined);
            result.addProperty("uniqueBlocks", uniqueBlocks);
            
            // 找出挖掘最多的方块
            Material mostMinedBlock = null;
            int mostMinedCount = 0;
            for (Map.Entry<Material, Integer> entry : stats.entrySet()) {
                if (entry.getValue() > mostMinedCount) {
                    mostMinedCount = entry.getValue();
                    mostMinedBlock = entry.getKey();
                }
            }
            
            if (mostMinedBlock != null) {
                result.addProperty("mostMinedBlock", mostMinedBlock.name());
                result.addProperty("mostMinedCount", mostMinedCount);
            }
        } else {
            result.addProperty("totalMined", 0);
            result.addProperty("uniqueBlocks", 0);
        }
        
        return result;
    }
    
    /**
     * 保存玩家统计数据
     */
    private void savePlayerStats(UUID playerId) {
        try {
            String fileName = playerId.toString() + ".json";
            File statsFile = new File(dataFolder, fileName);
            
            Map<Material, Integer> stats = playerMiningStats.get(playerId);
            if (stats == null) return;
            
            JsonObject data = new JsonObject();
            for (Map.Entry<Material, Integer> entry : stats.entrySet()) {
                data.addProperty(entry.getKey().name().toLowerCase(), entry.getValue());
            }
            
            try (FileWriter writer = new FileWriter(statsFile)) {
                gson.toJson(data, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("保存挖掘统计数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 加载玩家统计数据
     */
    private void loadPlayerStats(UUID playerId) {
        try {
            String fileName = playerId.toString() + ".json";
            File statsFile = new File(dataFolder, fileName);
            
            if (!statsFile.exists()) return;
            
            try (FileReader reader = new FileReader(statsFile)) {
                JsonObject data = gson.fromJson(reader, JsonObject.class);
                if (data != null) {
                    Map<Material, Integer> stats = new ConcurrentHashMap<>();
                    for (String blockName : data.keySet()) {
                        try {
                            Material material = Material.valueOf(blockName.toUpperCase());
                            int count = data.get(blockName).getAsInt();
                            stats.put(material, count);
                        } catch (IllegalArgumentException e) {
                            // 忽略无效的材料类型
                        }
                    }
                    playerMiningStats.put(playerId, stats);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("加载挖掘统计数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 加载所有玩家统计数据
     */
    private void loadAllStats() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    String fileName = file.getName();
                    String uuidString = fileName.substring(0, fileName.length() - 5); // 移除.json
                    UUID playerId = UUID.fromString(uuidString);
                    loadPlayerStats(playerId);
                } catch (IllegalArgumentException e) {
                    // 忽略无效的UUID文件
                }
            }
        }
    }
    
    /**
     * 获取所有挖掘统计
     */
    public JsonObject getAllStats() {
        JsonObject result = new JsonObject();
        JsonArray allStats = new JsonArray();
        
        for (Map.Entry<UUID, Map<Material, Integer>> entry : playerMiningStats.entrySet()) {
            String playerName = plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
            if (playerName != null) {
                JsonObject playerStat = new JsonObject();
                playerStat.addProperty("username", playerName);
                playerStat.addProperty("uuid", entry.getKey().toString());
                
                JsonObject blocks = new JsonObject();
                for (Map.Entry<Material, Integer> blockEntry : entry.getValue().entrySet()) {
                    blocks.addProperty(blockEntry.getKey().name().toLowerCase(), blockEntry.getValue());
                }
                playerStat.add("blocks", blocks);
                
                allStats.add(playerStat);
            }
        }
        
        result.add("allStats", allStats);
        return result;
    }
}