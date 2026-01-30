package com.example.userinfoapi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Material;

import java.util.Map;
import java.util.HashMap;

public class UserInfoService {
    
    private final Gson gson;
    
    public UserInfoService() {
        this.gson = new Gson();
    }
    
    public JsonObject getUserInfo(String username) {
        Player onlinePlayer = Bukkit.getPlayer(username);
        
        // 如果玩家在线，返回完整信息
        if (onlinePlayer != null) {
            JsonObject userInfo = new JsonObject();
            userInfo.addProperty("username", onlinePlayer.getName());
            userInfo.addProperty("uuid", onlinePlayer.getUniqueId().toString());
            userInfo.addProperty("displayName", onlinePlayer.getDisplayName());
            userInfo.addProperty("level", onlinePlayer.getLevel());
            userInfo.addProperty("exp", onlinePlayer.getExp());
            userInfo.addProperty("expToLevel", onlinePlayer.getExpToLevel());
            userInfo.add("location", getLocationJson(onlinePlayer.getLocation()));
            userInfo.add("inventory", getInventoryJson(onlinePlayer.getInventory()));
            userInfo.addProperty("health", onlinePlayer.getHealth());
            userInfo.addProperty("maxHealth", onlinePlayer.getMaxHealth());
            userInfo.addProperty("foodLevel", onlinePlayer.getFoodLevel());
            userInfo.addProperty("gameMode", onlinePlayer.getGameMode().toString());
            userInfo.addProperty("online", true);
            
            // 添加在线时长信息
            LoginRecordManager loginRecordManager = UserInfoAPIPlugin.getInstance().getLoginRecordManager();
            if (loginRecordManager != null) {
                userInfo.addProperty("currentSessionOnlineTime", loginRecordManager.getCurrentSessionOnlineTime(onlinePlayer.getName()));
                userInfo.addProperty("totalOnlineTime", loginRecordManager.getTotalOnlineTime(onlinePlayer.getName()));
            }
            
            return userInfo;
        }
        
        // 如果玩家离线，尝试获取离线玩家信息
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        if (offlinePlayer.hasPlayedBefore()) {
            JsonObject userInfo = new JsonObject();
            userInfo.addProperty("username", offlinePlayer.getName());
            userInfo.addProperty("uuid", offlinePlayer.getUniqueId().toString());
            userInfo.addProperty("displayName", offlinePlayer.getName());
            
            // 获取玩家统计数据（如果可用）
            int level = 0;
            double exp = 0.0;
            double expToLevel = 0;
            
            // 尝试获取离线玩家的最后位置和统计数据
            org.bukkit.Location lastLocation = null;
            
            try {
                // 获取离线玩家的最后位置
                lastLocation = offlinePlayer.getLastPlayed() > 0 ? offlinePlayer.getPlayer() != null ?
                    offlinePlayer.getPlayer().getLocation() : null : null;
                
                // 如果玩家当前在线，获取实时数据
                if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
                    level = offlinePlayer.getPlayer().getLevel();
                    exp = offlinePlayer.getPlayer().getExp();
                    expToLevel = offlinePlayer.getPlayer().getExpToLevel();
                }
            } catch (Exception e) {
                // 如果无法获取数据，使用默认值
            }
            
            userInfo.addProperty("level", level);
            userInfo.addProperty("exp", exp);
            userInfo.addProperty("expToLevel", expToLevel);
            userInfo.addProperty("health", 20.0); // 默认健康值
            userInfo.addProperty("maxHealth", 20.0); // 默认最大健康值
            userInfo.addProperty("foodLevel", 20); // 默认饱食度
            userInfo.addProperty("gameMode", "SURVIVAL"); // 离线玩家无法获取游戏模式，使用默认值
            
            // 添加最后位置信息
            if (lastLocation != null) {
                JsonObject location = new JsonObject();
                location.addProperty("x", lastLocation.getX());
                location.addProperty("y", lastLocation.getY());
                location.addProperty("z", lastLocation.getZ());
                location.addProperty("world", lastLocation.getWorld() != null ? lastLocation.getWorld().getName() : "world");
                location.addProperty("yaw", lastLocation.getYaw());
                location.addProperty("pitch", lastLocation.getPitch());
                userInfo.add("location", location);
            } else {
                // 尝试从玩家数据文件获取最后位置（更可靠的方法）
                JsonObject location = new JsonObject();
                location.addProperty("x", 0.0);
                location.addProperty("y", 0.0);
                location.addProperty("z", 0.0);
                location.addProperty("world", "world");
                location.addProperty("yaw", 0.0);
                location.addProperty("pitch", 0.0);
                userInfo.add("location", location);
            }
            
            userInfo.addProperty("online", false);
            
            // 添加离线玩家的额外信息
            userInfo.addProperty("firstPlayed", offlinePlayer.getFirstPlayed());
            userInfo.addProperty("lastPlayed", offlinePlayer.getLastPlayed());
            userInfo.addProperty("isOnline", offlinePlayer.isOnline());
            userInfo.addProperty("whitelisted", offlinePlayer.isWhitelisted());
            userInfo.addProperty("banned", offlinePlayer.isBanned());
            userInfo.addProperty("op", offlinePlayer.isOp());
            
            // 添加在线时长信息
            LoginRecordManager loginRecordManager = UserInfoAPIPlugin.getInstance().getLoginRecordManager();
            if (loginRecordManager != null) {
                userInfo.addProperty("currentSessionOnlineTime", 0); // 离线玩家当前会话时长为0
                userInfo.addProperty("totalOnlineTime", loginRecordManager.getTotalOnlineTime(offlinePlayer.getName()));
            }
            
            return userInfo;
        }
        
        return null;
    }
    
    public JsonObject getUserLevel(String username) {
        Player onlinePlayer = Bukkit.getPlayer(username);
        
        // 如果玩家在线，返回实时信息
        if (onlinePlayer != null) {
            JsonObject levelInfo = new JsonObject();
            levelInfo.addProperty("username", onlinePlayer.getName());
            levelInfo.addProperty("level", onlinePlayer.getLevel());
            levelInfo.addProperty("exp", onlinePlayer.getExp());
            levelInfo.addProperty("expToLevel", onlinePlayer.getExpToLevel());
            levelInfo.addProperty("totalExperience", onlinePlayer.getTotalExperience());
            levelInfo.addProperty("online", true);
            
            // 添加在线时长信息
            LoginRecordManager loginRecordManager = UserInfoAPIPlugin.getInstance().getLoginRecordManager();
            if (loginRecordManager != null) {
                levelInfo.addProperty("currentSessionOnlineTime", loginRecordManager.getCurrentSessionOnlineTime(onlinePlayer.getName()));
                levelInfo.addProperty("totalOnlineTime", loginRecordManager.getTotalOnlineTime(onlinePlayer.getName()));
            }
            
            return levelInfo;
        }
        
        // 如果玩家离线，尝试获取离线玩家信息
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        if (offlinePlayer.hasPlayedBefore()) {
            JsonObject levelInfo = new JsonObject();
            levelInfo.addProperty("username", offlinePlayer.getName());
            
            // 尝试获取离线玩家的等级数据
            int level = 0;
            double exp = 0.0;
            double expToLevel = 0;
            int totalExperience = 0;
            
            try {
                // 如果玩家当前在线，获取实时数据
                if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
                    level = offlinePlayer.getPlayer().getLevel();
                    exp = offlinePlayer.getPlayer().getExp();
                    expToLevel = offlinePlayer.getPlayer().getExpToLevel();
                    totalExperience = offlinePlayer.getPlayer().getTotalExperience();
                }
            } catch (Exception e) {
                // 如果无法获取实时数据，使用默认值
            }
            
            levelInfo.addProperty("level", level);
            levelInfo.addProperty("exp", exp);
            levelInfo.addProperty("expToLevel", expToLevel);
            levelInfo.addProperty("totalExperience", totalExperience);
            levelInfo.addProperty("online", false);
            
            // 添加离线玩家的额外信息
            levelInfo.addProperty("firstPlayed", offlinePlayer.getFirstPlayed());
            levelInfo.addProperty("lastPlayed", offlinePlayer.getLastPlayed());
            levelInfo.addProperty("isOnline", offlinePlayer.isOnline());
            
            // 添加在线时长信息
            LoginRecordManager loginRecordManager = UserInfoAPIPlugin.getInstance().getLoginRecordManager();
            if (loginRecordManager != null) {
                levelInfo.addProperty("currentSessionOnlineTime", 0); // 离线玩家当前会话时长为0
                levelInfo.addProperty("totalOnlineTime", loginRecordManager.getTotalOnlineTime(offlinePlayer.getName()));
            }
            
            return levelInfo;
        }
        
        return null;
    }
    
    public JsonObject getUserLocation(String username) {
        Player onlinePlayer = Bukkit.getPlayer(username);
        
        // 如果玩家在线，返回实时位置信息
        if (onlinePlayer != null) {
            JsonObject locationInfo = new JsonObject();
            locationInfo.addProperty("username", onlinePlayer.getName());
            locationInfo.add("location", getLocationJson(onlinePlayer.getLocation()));
            locationInfo.addProperty("world", onlinePlayer.getWorld().getName());
            locationInfo.addProperty("biome", onlinePlayer.getLocation().getBlock().getBiome().toString());
            locationInfo.addProperty("online", true);
            
            // 添加在线时长信息
            LoginRecordManager loginRecordManager = UserInfoAPIPlugin.getInstance().getLoginRecordManager();
            if (loginRecordManager != null) {
                locationInfo.addProperty("currentSessionOnlineTime", loginRecordManager.getCurrentSessionOnlineTime(onlinePlayer.getName()));
                locationInfo.addProperty("totalOnlineTime", loginRecordManager.getTotalOnlineTime(onlinePlayer.getName()));
            }
            
            return locationInfo;
        }
        
        // 如果玩家离线，尝试获取离线玩家信息
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        if (offlinePlayer.hasPlayedBefore()) {
            JsonObject locationInfo = new JsonObject();
            locationInfo.addProperty("username", offlinePlayer.getName());
            
            // 尝试获取离线玩家的最后位置
            org.bukkit.Location lastLocation = null;
            String worldName = "world";
            String biomeName = "PLAINS";
            
            try {
                // 如果玩家当前在线，获取实时位置
                if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
                    lastLocation = offlinePlayer.getPlayer().getLocation();
                    worldName = offlinePlayer.getPlayer().getWorld().getName();
                    biomeName = offlinePlayer.getPlayer().getLocation().getBlock().getBiome().toString();
                } else {
                    // 获取离线玩家的最后位置
                    lastLocation = offlinePlayer.getLastPlayed() > 0 ?
                        offlinePlayer.getPlayer() != null ? offlinePlayer.getPlayer().getLocation() : null : null;
                    if (lastLocation != null) {
                        worldName = lastLocation.getWorld() != null ? lastLocation.getWorld().getName() : "world";
                        if (lastLocation.getWorld() != null) {
                            biomeName = lastLocation.getBlock().getBiome().toString();
                        }
                    }
                }
            } catch (Exception e) {
                // 如果无法获取位置信息，使用默认值
            }
            
            // 添加位置信息
            if (lastLocation != null) {
                JsonObject location = new JsonObject();
                location.addProperty("x", lastLocation.getX());
                location.addProperty("y", lastLocation.getY());
                location.addProperty("z", lastLocation.getZ());
                location.addProperty("yaw", lastLocation.getYaw());
                location.addProperty("pitch", lastLocation.getPitch());
                locationInfo.add("location", location);
            } else {
                // 使用默认位置
                JsonObject location = new JsonObject();
                location.addProperty("x", 0.0);
                location.addProperty("y", 0.0);
                location.addProperty("z", 0.0);
                location.addProperty("yaw", 0.0);
                location.addProperty("pitch", 0.0);
                locationInfo.add("location", location);
            }
            
            locationInfo.addProperty("world", worldName);
            locationInfo.addProperty("biome", biomeName);
            locationInfo.addProperty("online", false);
            
            // 添加离线玩家的额外信息
            locationInfo.addProperty("firstPlayed", offlinePlayer.getFirstPlayed());
            locationInfo.addProperty("lastPlayed", offlinePlayer.getLastPlayed());
            locationInfo.addProperty("isOnline", offlinePlayer.isOnline());
            
            // 添加在线时长信息
            LoginRecordManager loginRecordManager = UserInfoAPIPlugin.getInstance().getLoginRecordManager();
            if (loginRecordManager != null) {
                locationInfo.addProperty("currentSessionOnlineTime", 0); // 离线玩家当前会话时长为0
                locationInfo.addProperty("totalOnlineTime", loginRecordManager.getTotalOnlineTime(offlinePlayer.getName()));
            }
            
            return locationInfo;
        }
        
        return null;
    }
    
    public JsonObject getUserInventory(String username) {
        Player onlinePlayer = Bukkit.getPlayer(username);
        
        // 如果玩家在线，返回实时背包信息
        if (onlinePlayer != null) {
            JsonObject inventoryInfo = new JsonObject();
            inventoryInfo.addProperty("username", onlinePlayer.getName());
            inventoryInfo.add("inventory", getInventoryJson(onlinePlayer.getInventory()));
            inventoryInfo.addProperty("online", true);
            
            // 添加在线时长信息
            LoginRecordManager loginRecordManager = UserInfoAPIPlugin.getInstance().getLoginRecordManager();
            if (loginRecordManager != null) {
                inventoryInfo.addProperty("currentSessionOnlineTime", loginRecordManager.getCurrentSessionOnlineTime(onlinePlayer.getName()));
                inventoryInfo.addProperty("totalOnlineTime", loginRecordManager.getTotalOnlineTime(onlinePlayer.getName()));
            }
            
            return inventoryInfo;
        }
        
        // 如果玩家离线，尝试获取离线玩家信息
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        if (offlinePlayer.hasPlayedBefore()) {
            JsonObject inventoryInfo = new JsonObject();
            inventoryInfo.addProperty("username", offlinePlayer.getName());
            inventoryInfo.add("inventory", new JsonArray()); // 离线玩家无法获取实时背包
            inventoryInfo.addProperty("online", false);
            
            // 添加离线玩家的额外信息
            inventoryInfo.addProperty("firstPlayed", offlinePlayer.getFirstPlayed());
            inventoryInfo.addProperty("lastPlayed", offlinePlayer.getLastPlayed());
            inventoryInfo.addProperty("isOnline", offlinePlayer.isOnline());
            
            // 添加在线时长信息
            LoginRecordManager loginRecordManager = UserInfoAPIPlugin.getInstance().getLoginRecordManager();
            if (loginRecordManager != null) {
                inventoryInfo.addProperty("currentSessionOnlineTime", 0); // 离线玩家当前会话时长为0
                inventoryInfo.addProperty("totalOnlineTime", loginRecordManager.getTotalOnlineTime(offlinePlayer.getName()));
            }
            
            return inventoryInfo;
        }
        
        return null;
    }
    
    private JsonObject getLocationJson(Location location) {
        JsonObject loc = new JsonObject();
        loc.addProperty("x", location.getX());
        loc.addProperty("y", location.getY());
        loc.addProperty("z", location.getZ());
        loc.addProperty("yaw", location.getYaw());
        loc.addProperty("pitch", location.getPitch());
        return loc;
    }
    
    private JsonArray getInventoryJson(PlayerInventory inventory) {
        JsonArray items = new JsonArray();
        
        // 主背包物品 (0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(getItemJson(item, i, "main"));
            }
        }
        
        // 盔甲槽物品 (36-39)
        ItemStack[] armor = inventory.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && armor[i].getType() != Material.AIR) {
                items.add(getItemJson(armor[i], 36 + i, "armor"));
            }
        }
        
        // 副手物品 (40)
        ItemStack offHand = inventory.getItemInOffHand();
        if (offHand != null && offHand.getType() != Material.AIR) {
            items.add(getItemJson(offHand, 40, "offhand"));
        }
        
        return items;
    }
    
    private JsonObject getItemJson(ItemStack item, int slot, String slotType) {
        JsonObject itemJson = new JsonObject();
        itemJson.addProperty("slot", slot);
        itemJson.addProperty("slotType", slotType);
        itemJson.addProperty("type", item.getType().toString());
        itemJson.addProperty("amount", item.getAmount());
        itemJson.addProperty("displayName", item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
            ? item.getItemMeta().getDisplayName() 
            : item.getType().toString());
        
        // 物品耐久度
        if (item.getDurability() != 0) {
            itemJson.addProperty("durability", item.getDurability());
            itemJson.addProperty("maxDurability", item.getType().getMaxDurability());
        }
        
        // 附魔信息
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            JsonArray enchantments = new JsonArray();
            Map<Enchantment, Integer> enchants = item.getEnchantments();
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                JsonObject enchant = new JsonObject();
                enchant.addProperty("name", entry.getKey().getKey().getKey());
                enchant.addProperty("level", entry.getValue());
                enchantments.add(enchant);
            }
            itemJson.add("enchantments", enchantments);
        }
        
        // 物品描述
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            JsonArray lore = new JsonArray();
            for (String line : item.getItemMeta().getLore()) {
                lore.add(line);
            }
            itemJson.add("lore", lore);
        }
        
        return itemJson;
    }
}