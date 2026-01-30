package com.example.userinfoapi;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class UserInfoAPIPlugin extends JavaPlugin {
    
    private static UserInfoAPIPlugin instance;
    private APIServer apiServer;
    private Logger logger;
    private SecurityManager securityManager;
    private LoginRecordManager loginRecordManager;
    private MiningStatsManager miningStatsManager;
    
    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        try {
            // 保存默认配置
            saveDefaultConfig();
            
            // 检查并修复配置
            validateAndFixConfig();
            
            // 初始化安全管理器
            securityManager = new SecurityManager(this);
            
            // 初始化登录记录管理器
            loginRecordManager = new LoginRecordManager(this);
            getServer().getPluginManager().registerEvents(loginRecordManager, this);
            
            // 初始化挖掘统计管理器
            miningStatsManager = new MiningStatsManager(this);
            getServer().getPluginManager().registerEvents(miningStatsManager, this);
            
            // 启动API服务器
            startAPIServer();
            
            // 注册命令
            this.getCommand("userinfo").setExecutor(new UserInfoCommand(this));
            
            // 显示启动信息
            displayStartupInfo();
            
            logger.info("UserInfoAPI 插件已成功启用！");
            
        } catch (Exception e) {
            logger.severe("插件启用失败: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        // 停止API服务器
        if (apiServer != null) {
            apiServer.stopServer();
        }
        
        logger.info("UserInfoAPI 插件已禁用！");
    }
    
    private void startAPIServer() {
        int port = getConfig().getInt("api.port", 8080);
        String host = getConfig().getString("api.host", "0.0.0.0");
        
        apiServer = new APIServer(this, host, port);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    apiServer.start();
                    logger.info("API服务器已启动在 " + host + ":" + port);
                } catch (Exception e) {
                    logger.severe("启动API服务器失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(this);
    }
    
    public void restartAPIServer() {
        if (apiServer != null) {
            apiServer.stopServer();
        }
        startAPIServer();
    }
    
    /**
     * 验证并修复配置文件
     */
    private void validateAndFixConfig() {
        logger.info("正在检查配置文件...");
        
        // 检查API配置
        if (!getConfig().contains("api.host")) {
            getConfig().set("api.host", "0.0.0.0");
            logger.warning("API主机配置缺失，已设置为默认值: 0.0.0.0");
        }
        
        if (!getConfig().contains("api.port")) {
            getConfig().set("api.port", 8080);
            logger.warning("API端口配置缺失，已设置为默认值: 8080");
        }
        
        // 检查安全配置
        if (!getConfig().contains("security.enabled")) {
            getConfig().set("security.enabled", false);
            logger.warning("安全配置缺失，已设置为默认值: false");
        }
        
        // 检查限流配置
        if (!getConfig().contains("rate-limit.enabled")) {
            getConfig().set("rate-limit.enabled", true);
            getConfig().set("rate-limit.requests-per-minute", 60);
            getConfig().set("rate-limit.requests-per-hour", 1000);
            logger.warning("限流配置缺失，已设置为默认值");
        }
        
        // 检查API密钥
        if (!getConfig().contains("security.api-keys") || getConfig().getMapList("security.api-keys").isEmpty()) {
            String defaultKey = "UK_default_key_" + System.currentTimeMillis();
            List<Map<String, Object>> apiKeys = new ArrayList<>();
            Map<String, Object> keyInfo = new HashMap<>();
            keyInfo.put("key", defaultKey);
            keyInfo.put("name", "默认密钥");
            keyInfo.put("description", "自动生成的基础密钥");
            keyInfo.put("active", true);
            apiKeys.add(keyInfo);
            
            getConfig().set("security.api-keys", apiKeys);
            logger.warning("API密钥配置缺失，已生成默认密钥: " + defaultKey);
            logger.warning("为了安全起见，请尽快修改默认密钥！");
        }
        
        // 保存修复后的配置
        saveConfig();
        logger.info("配置文件检查完成");
    }
    
    /**
     * 显示启动信息
     */
    private void displayStartupInfo() {
        logger.info("=== UserInfoAPI 启动信息 ===");
        logger.info("插件版本: 2.0");
        logger.info("作者: httye");
        logger.info("API服务器: " + getConfig().getString("api.host") + ":" + getConfig().getInt("api.port"));
        logger.info("安全功能: " + (getConfig().getBoolean("security.enabled") ? "启用" : "禁用"));
        logger.info("限流功能: " + (getConfig().getBoolean("rate-limit.enabled") ? "启用" : "禁用"));
        logger.info("批量查询: " + (getConfig().getBoolean("batch-query.enabled") ? "启用" : "禁用"));
        logger.info("数据导出: " + (getConfig().getBoolean("data-export.enabled") ? "启用" : "禁用"));
        
        // 显示API密钥信息
        if (getConfig().getBoolean("security.enabled")) {
            List<?> apiKeys = getConfig().getMapList("security.api-keys");
            int activeKeys = 0;
            for (Object keyObj : apiKeys) {
                if (keyObj instanceof Map) {
                    Map<?, ?> keyInfo = (Map<?, ?>) keyObj;
                    if (Boolean.TRUE.equals(keyInfo.get("active"))) {
                        activeKeys++;
                    }
                }
            }
            logger.info("活跃API密钥数量: " + activeKeys);
        }
        
        logger.info("==========================");
    }
    
    /**
     * 获取API服务器状态
     */
    public String getApiServerStatus() {
        if (apiServer == null) {
            return "未运行";
        }
        return "运行中 (端口: " + getConfig().getInt("api.port") + ")";
    }
    
    /**
     * 检查API服务器是否正常运行
     */
    public boolean isApiServerRunning() {
        return apiServer != null;
    }
    
    public static UserInfoAPIPlugin getInstance() {
        return instance;
    }
    
    public APIServer getApiServer() {
        return apiServer;
    }
    
    public SecurityManager getSecurityManager() {
        return securityManager;
    }
    
    public LoginRecordManager getLoginRecordManager() {
        return loginRecordManager;
    }
    
    public MiningStatsManager getMiningStatsManager() {
        return miningStatsManager;
    }
    
    public RateLimitHandler getRateLimitHandler() {
        return apiServer != null ? apiServer.getRateLimitHandler() : null;
    }
    
    public com.google.gson.Gson getGson() {
        return new com.google.gson.Gson();
    }
}