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
            
            return userInfo;
        }
        
        // 如果玩家离线，尝试获取离线玩家信息
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        if (offlinePlayer.hasPlayedBefore()) {
            JsonObject userInfo = new JsonObject();
            userInfo.addProperty("username", offlinePlayer.getName());
            userInfo.addProperty("uuid", offlinePlayer.getUniqueId().toString());
            userInfo.addProperty("displayName", offlinePlayer.getName());
            userInfo.addProperty("level", 0); // 离线玩家无法获取实时等级
            userInfo.addProperty("exp", 0.0); // 离线玩家无法获取实时经验
            userInfo.addProperty("expToLevel", 0); // 离线玩家无法获取实时经验
            userInfo.addProperty("health", 20.0); // 默认健康值
            userInfo.addProperty("maxHealth", 20.0); // 默认最大健康值
            userInfo.addProperty("foodLevel", 20); // 默认饱食度
            userInfo.addProperty("gameMode", "UNKNOWN"); // 离线玩家无法获取游戏模式
            userInfo.addProperty("online", false);
            
            // 添加离线玩家的额外信息
            userInfo.addProperty("firstPlayed", offlinePlayer.getFirstPlayed());
            userInfo.addProperty("lastPlayed", offlinePlayer.getLastPlayed());
            userInfo.addProperty("isOnline", offlinePlayer.isOnline());
            userInfo.addProperty("whitelisted", offlinePlayer.isWhitelisted());
            userInfo.addProperty("banned", offlinePlayer.isBanned());
            userInfo.addProperty("op", offlinePlayer.isOp());
            
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
            
            return levelInfo;
        }
        
        // 如果玩家离线，尝试获取离线玩家信息
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        if (offlinePlayer.hasPlayedBefore()) {
            JsonObject levelInfo = new JsonObject();
            levelInfo.addProperty("username", offlinePlayer.getName());
            levelInfo.addProperty("level", 0); // 离线玩家无法获取实时等级
            levelInfo.addProperty("exp", 0.0); // 离线玩家无法获取实时经验
            levelInfo.addProperty("expToLevel", 0); // 离线玩家无法获取实时经验
            levelInfo.addProperty("totalExperience", 0); // 离线玩家无法获取总经验
            levelInfo.addProperty("online", false);
            
            // 添加离线玩家的额外信息
            levelInfo.addProperty("firstPlayed", offlinePlayer.getFirstPlayed());
            levelInfo.addProperty("lastPlayed", offlinePlayer.getLastPlayed());
            levelInfo.addProperty("isOnline", offlinePlayer.isOnline());
            
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
            
            return locationInfo;
        }
        
        // 如果玩家离线，尝试获取离线玩家信息
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        if (offlinePlayer.hasPlayedBefore()) {
            JsonObject locationInfo = new JsonObject();
            locationInfo.addProperty("username", offlinePlayer.getName());
            locationInfo.addProperty("world", "UNKNOWN"); // 离线玩家无法获取实时世界
            locationInfo.addProperty("biome", "UNKNOWN"); // 离线玩家无法获取实时生物群系
            locationInfo.addProperty("online", false);
            
            // 添加离线玩家的额外信息
            locationInfo.addProperty("firstPlayed", offlinePlayer.getFirstPlayed());
            locationInfo.addProperty("lastPlayed", offlinePlayer.getLastPlayed());
            locationInfo.addProperty("isOnline", offlinePlayer.isOnline());
            
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