package com.httye.userinfoapi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityManager {
    
    private final UserInfoAPIPlugin plugin;
    private final Gson gson;
    private final Map<String, ApiKey> apiKeys;
    private final Set<String> allowedIPs;
    private final SecureRandom random;
    private boolean securityEnabled;
    
    public SecurityManager(UserInfoAPIPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.apiKeys = new ConcurrentHashMap<>();
        this.allowedIPs = ConcurrentHashMap.newKeySet();
        this.random = new SecureRandom();
        this.securityEnabled = plugin.getConfig().getBoolean("security.enabled", false);
        
        loadApiKeys();
        loadAllowedIPs();
    }
    
    public boolean isSecurityEnabled() {
        return securityEnabled;
    }
    
    public void setSecurityEnabled(boolean enabled) {
        this.securityEnabled = enabled;
        plugin.getConfig().set("security.enabled", enabled);
        plugin.saveConfig();
    }
    
    public String generateApiKey(String name, String description) {
        String key = generateSecureKey();
        ApiKey apiKey = new ApiKey(key, name, description, new Date(), true);
        apiKeys.put(key, apiKey);
        saveApiKeys();
        return key;
    }
    
    public boolean validateApiKey(String key) {
        if (!securityEnabled) {
            return true; // 如果安全功能关闭，允许所有请求
        }
        
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        ApiKey apiKey = apiKeys.get(key);
        return apiKey != null && apiKey.isActive();
    }
    
    public boolean validateIPAddress(String ipAddress) {
        if (!securityEnabled) {
            return true;
        }
        
        if (allowedIPs.isEmpty()) {
            return true; // 如果没有设置允许的IP，允许所有IP
        }
        
        return allowedIPs.contains(ipAddress) || isIPInRange(ipAddress);
    }
    
    public JsonObject getSecurityInfo() {
        JsonObject info = new JsonObject();
        info.addProperty("securityEnabled", securityEnabled);
        info.addProperty("totalApiKeys", apiKeys.size());
        info.addProperty("activeApiKeys", getActiveApiKeyCount());
        info.addProperty("allowedIPs", allowedIPs.size());
        return info;
    }
    
    public JsonObject listApiKeys() {
        JsonObject result = new JsonObject();
        JsonArray keys = new JsonArray();
        
        for (ApiKey apiKey : apiKeys.values()) {
            JsonObject key = new JsonObject();
            key.addProperty("name", apiKey.getName());
            key.addProperty("description", apiKey.getDescription());
            key.addProperty("created", apiKey.getCreated().toString());
            key.addProperty("active", apiKey.isActive());
            key.addProperty("lastUsed", apiKey.getLastUsed() != null ? apiKey.getLastUsed().toString() : "从未使用");
            keys.add(key);
        }
        
        result.add("apiKeys", keys);
        return result;
    }
    
    public boolean revokeApiKey(String key) {
        ApiKey apiKey = apiKeys.remove(key);
        if (apiKey != null) {
            saveApiKeys();
            return true;
        }
        return false;
    }
    
    public void recordApiKeyUsage(String key) {
        ApiKey apiKey = apiKeys.get(key);
        if (apiKey != null) {
            apiKey.setLastUsed(new Date());
            saveApiKeys();
        }
    }
    
    private String generateSecureKey() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder key = new StringBuilder("UK_"); // API Key 前缀
        
        for (int i = 0; i < 32; i++) {
            key.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return key.toString();
    }
    
    private void loadApiKeys() {
        File keysFile = new File(plugin.getDataFolder(), "api_keys.json");
        if (keysFile.exists()) {
            try (FileReader reader = new FileReader(keysFile)) {
                Map<String, ApiKey> loadedKeys = gson.fromJson(reader, 
                    new TypeToken<Map<String, ApiKey>>(){}.getType());
                if (loadedKeys != null) {
                    apiKeys.putAll(loadedKeys);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("加载API密钥失败: " + e.getMessage());
            }
        }
    }
    
    private void saveApiKeys() {
        File keysFile = new File(plugin.getDataFolder(), "api_keys.json");
        try (FileWriter writer = new FileWriter(keysFile)) {
            gson.toJson(apiKeys, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("保存API密钥失败: " + e.getMessage());
        }
    }
    
    private void loadAllowedIPs() {
        List<String> ips = plugin.getConfig().getStringList("security.allowed-ips");
        allowedIPs.addAll(ips);
    }
    
    private boolean isIPInRange(String ipAddress) {
        // 简单的IP范围检查，支持 CIDR 表示法
        for (String allowedIP : allowedIPs) {
            if (allowedIP.contains("/")) {
                // CIDR 表示法，如 192.168.1.0/24
                if (isIPInCIDR(ipAddress, allowedIP)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isIPInCIDR(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String baseIP = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            
            long ipLong = ipToLong(ip);
            long baseIPLong = ipToLong(baseIP);
            long mask = -1 << (32 - prefixLength);
            
            return (ipLong & mask) == (baseIPLong & mask);
        } catch (Exception e) {
            return false;
        }
    }
    
    private long ipToLong(String ipAddress) {
        String[] ipParts = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = result << 8;
            result |= Integer.parseInt(ipParts[i]);
        }
        return result;
    }
    
    private int getActiveApiKeyCount() {
        int count = 0;
        for (ApiKey apiKey : apiKeys.values()) {
            if (apiKey.isActive()) {
                count++;
            }
        }
        return count;
    }
    
    private static class ApiKey {
        private final String key;
        private final String name;
        private final String description;
        private final Date created;
        private boolean active;
        private Date lastUsed;
        
        public ApiKey(String key, String name, String description, Date created, boolean active) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.created = created;
            this.active = active;
        }
        
        public String getKey() { return key; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Date getCreated() { return created; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public Date getLastUsed() { return lastUsed; }
        public void setLastUsed(Date lastUsed) { this.lastUsed = lastUsed; }
    }
}