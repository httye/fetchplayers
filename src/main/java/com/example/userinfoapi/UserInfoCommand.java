package com.example.userinfoapi;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import com.google.gson.JsonArray;

public class UserInfoCommand implements CommandExecutor {
    
    private final UserInfoAPIPlugin plugin;
    
    public UserInfoCommand(UserInfoAPIPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReload(sender);
            case "status":
                return handleStatus(sender);
            case "key":
                return handleApiKey(sender, args);
            case "security":
                return handleSecurity(sender, args);
            case "help":
                sendHelp(sender);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "未知命令。使用 /userinfo help 查看帮助。");
                return true;
        }
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("userinfoapi.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return true;
        }
        
        try {
            plugin.reloadConfig();
            plugin.restartAPIServer();
            sender.sendMessage(ChatColor.GREEN + "UserInfoAPI 配置已重载，API服务器已重启。");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "重载失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("userinfoapi.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return true;
        }
        
        APIServer apiServer = plugin.getApiServer();
        
        sender.sendMessage(ChatColor.GOLD + "=== UserInfoAPI 状态 ===");
        sender.sendMessage(ChatColor.YELLOW + "插件状态: " + ChatColor.GREEN + "已启用");
        
        if (apiServer != null) {
            sender.sendMessage(ChatColor.YELLOW + "API服务器: " + ChatColor.GREEN + "运行中");
            sender.sendMessage(ChatColor.YELLOW + "监听地址: " + ChatColor.WHITE + 
                plugin.getConfig().getString("api.host", "0.0.0.0") + ":" + 
                plugin.getConfig().getInt("api.port", 8080));
        } else {
            sender.sendMessage(ChatColor.YELLOW + "API服务器: " + ChatColor.RED + "未运行");
        }
        
        sender.sendMessage(ChatColor.YELLOW + "可用API端点:");
        sender.sendMessage(ChatColor.WHITE + "  - /api/status (GET)");
        sender.sendMessage(ChatColor.WHITE + "  - /api/user/info?username=<玩家名> (GET)");
        sender.sendMessage(ChatColor.WHITE + "  - /api/user/level?username=<玩家名> (GET)");
        sender.sendMessage(ChatColor.WHITE + "  - /api/user/location?username=<玩家名> (GET)");
        sender.sendMessage(ChatColor.WHITE + "  - /api/user/inventory?username=<玩家名> (GET)");
        
        return true;
    }
    
    private boolean handleApiKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("userinfoapi.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /userinfo key <generate|list|revoke> [参数]");
            return true;
        }
        
        SecurityManager securityManager = plugin.getSecurityManager();
        
        switch (args[1].toLowerCase()) {
            case "generate":
                String name = args.length > 2 ? args[2] : "默认密钥";
                String description = args.length > 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)) : "";
                String apiKey = securityManager.generateApiKey(name, description);
                sender.sendMessage(ChatColor.GREEN + "API密钥生成成功: " + ChatColor.YELLOW + apiKey);
                sender.sendMessage(ChatColor.GRAY + "请妥善保存此密钥，它不会再次显示。");
                break;
                
            case "list":
                sender.sendMessage(ChatColor.GOLD + "=== API密钥列表 ===");
                com.google.gson.JsonObject keys = securityManager.listApiKeys();
                com.google.gson.JsonArray keyArray = keys.getAsJsonArray("apiKeys");
                for (int i = 0; i < keyArray.size(); i++) {
                    com.google.gson.JsonObject key = keyArray.get(i).getAsJsonObject();
                    sender.sendMessage(ChatColor.YELLOW + "- " + key.get("name").getAsString() +
                        (key.get("active").getAsBoolean() ? ChatColor.GREEN + " [活跃]" : ChatColor.RED + " [禁用]"));
                }
                break;
                
            case "revoke":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "用法: /userinfo key revoke <api_key>");
                    return true;
                }
                String keyToRevoke = args[2];
                if (securityManager.revokeApiKey(keyToRevoke)) {
                    sender.sendMessage(ChatColor.GREEN + "API密钥已撤销。");
                } else {
                    sender.sendMessage(ChatColor.RED + "未找到指定的API密钥。");
                }
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "未知子命令。可用: generate, list, revoke");
        }
        
        return true;
    }
    
    private boolean handleSecurity(CommandSender sender, String[] args) {
        if (!sender.hasPermission("userinfoapi.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return true;
        }
        
        SecurityManager securityManager = plugin.getSecurityManager();
        
        if (args.length < 2) {
            // 显示安全状态
            com.google.gson.JsonObject info = securityManager.getSecurityInfo();
            sender.sendMessage(ChatColor.GOLD + "=== 安全状态 ===");
            sender.sendMessage(ChatColor.YELLOW + "安全功能: " +
                (info.get("securityEnabled").getAsBoolean() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
            sender.sendMessage(ChatColor.YELLOW + "活跃API密钥: " + ChatColor.WHITE + info.get("activeApiKeys").getAsInt());
            sender.sendMessage(ChatColor.YELLOW + "允许的IP: " + ChatColor.WHITE + info.get("allowedIPs").getAsInt());
            return true;
        }
        
        switch (args[1].toLowerCase()) {
            case "enable":
                securityManager.setSecurityEnabled(true);
                sender.sendMessage(ChatColor.GREEN + "安全功能已启用。");
                break;
                
            case "disable":
                securityManager.setSecurityEnabled(false);
                sender.sendMessage(ChatColor.YELLOW + "安全功能已禁用。");
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "用法: /userinfo security [enable|disable]");
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== UserInfoAPI 帮助 ===");
        sender.sendMessage(ChatColor.YELLOW + "/userinfo reload - 重载配置和重启API服务器");
        sender.sendMessage(ChatColor.YELLOW + "/userinfo status - 查看插件状态");
        sender.sendMessage(ChatColor.YELLOW + "/userinfo key <generate|list|revoke> - 管理API密钥");
        sender.sendMessage(ChatColor.YELLOW + "/userinfo security [enable|disable] - 管理安全设置");
        sender.sendMessage(ChatColor.YELLOW + "/userinfo help - 显示此帮助信息");
        sender.sendMessage(ChatColor.GRAY + "需要权限: userinfoapi.admin");
    }
}