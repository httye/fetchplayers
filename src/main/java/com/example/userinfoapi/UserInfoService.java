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
        Player player = Bukkit.getPlayer(username);
        if (player == null) {
            return null;
        }
        
        JsonObject userInfo = new JsonObject();
        userInfo.addProperty("username", player.getName());
        userInfo.addProperty("uuid", player.getUniqueId().toString());
        userInfo.addProperty("displayName", player.getDisplayName());
        userInfo.addProperty("level", player.getLevel());
        userInfo.addProperty("exp", player.getExp());
        userInfo.addProperty("expToLevel", player.getExpToLevel());
        userInfo.add("location", getLocationJson(player.getLocation()));
        userInfo.add("inventory", getInventoryJson(player.getInventory()));
        userInfo.addProperty("health", player.getHealth());
        userInfo.addProperty("maxHealth", player.getMaxHealth());
        userInfo.addProperty("foodLevel", player.getFoodLevel());
        userInfo.addProperty("gameMode", player.getGameMode().toString());
        
        return userInfo;
    }
    
    public JsonObject getUserLevel(String username) {
        Player player = Bukkit.getPlayer(username);
        if (player == null) {
            return null;
        }
        
        JsonObject levelInfo = new JsonObject();
        levelInfo.addProperty("username", player.getName());
        levelInfo.addProperty("level", player.getLevel());
        levelInfo.addProperty("exp", player.getExp());
        levelInfo.addProperty("expToLevel", player.getExpToLevel());
        levelInfo.addProperty("totalExperience", player.getTotalExperience());
        
        return levelInfo;
    }
    
    public JsonObject getUserLocation(String username) {
        Player player = Bukkit.getPlayer(username);
        if (player == null) {
            return null;
        }
        
        JsonObject locationInfo = new JsonObject();
        locationInfo.addProperty("username", player.getName());
        locationInfo.add("location", getLocationJson(player.getLocation()));
        locationInfo.addProperty("world", player.getWorld().getName());
        locationInfo.addProperty("biome", player.getLocation().getBlock().getBiome().toString());
        
        return locationInfo;
    }
    
    public JsonObject getUserInventory(String username) {
        Player player = Bukkit.getPlayer(username);
        if (player == null) {
            return null;
        }
        
        JsonObject inventoryInfo = new JsonObject();
        inventoryInfo.addProperty("username", player.getName());
        inventoryInfo.add("inventory", getInventoryJson(player.getInventory()));
        
        return inventoryInfo;
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