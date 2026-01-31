package com.httye.userinfoapi;

import org.bukkit.Bukkit;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;

/**
 * 服务器资源监控器
 * 监控服务器内存和CPU使用情况
 */
public class ServerResourceMonitor {

    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;
    private final DecimalFormat decimalFormat;

    public ServerResourceMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.decimalFormat = new DecimalFormat("#.##");
    }

    /**
     * 获取内存使用情况
     */
    public ResourceInfo getMemoryInfo() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        long usedMemory = heapUsage.getUsed();
        long maxMemory = heapUsage.getMax();
        long committedMemory = heapUsage.getCommitted();

        double memoryUsagePercent = (maxMemory > 0) ?
                (double) usedMemory / maxMemory * 100 : 0;

        ResourceInfo info = new ResourceInfo();
        info.setType("memory");
        info.setUsed(formatBytes(usedMemory));
        info.setMax(formatBytes(maxMemory));
        info.setCommitted(formatBytes(committedMemory));
        info.setUsagePercent(decimalFormat.format(memoryUsagePercent));
        info.setFree(formatBytes(maxMemory - usedMemory));
        info.setNonHeapUsed(formatBytes(nonHeapUsage.getUsed()));
        info.setNonHeapMax(formatBytes(nonHeapUsage.getMax()));

        return info;
    }

    /**
     * 获取CPU使用情况
     */
    public ResourceInfo getCpuInfo() {
        ResourceInfo info = new ResourceInfo();
        info.setType("cpu");

        // 可用处理器数量
        int availableProcessors = osBean.getAvailableProcessors();
        info.setAvailableProcessors(availableProcessors);

        // 系统负载平均值
        double systemLoadAverage = osBean.getSystemLoadAverage();
        info.setSystemLoadAverage(decimalFormat.format(systemLoadAverage));

        // CPU使用率（基于负载平均值）
        double cpuUsagePercent = (availableProcessors > 0) ?
                (systemLoadAverage / availableProcessors) * 100 : 0;
        info.setUsagePercent(decimalFormat.format(cpuUsagePercent));

        // 系统信息
        info.setOsName(osBean.getName());
        info.setOsVersion(osBean.getVersion());
        info.setOsArch(osBean.getArch());

        return info;
    }

    /**
     * 获取TPS（每秒刻数）
     */
    public ResourceInfo getTpsInfo() {
        ResourceInfo info = new ResourceInfo();
        info.setType("tps");

        // 使用Bukkit API获取TPS
        try {
            // Paper API 提供了 getTPS() 方法
            double[] tps = Bukkit.getTPS();
            if (tps != null && tps.length > 0) {
                info.setCurrentTps(decimalFormat.format(tps[0])); // 最近1分钟
                info.setTps1m(decimalFormat.format(tps[0]));
                info.setTps5m(decimalFormat.format(tps[1]));
                info.setTps15m(decimalFormat.format(tps[2]));
            } else {
                info.setCurrentTps("20.00");
                info.setTps1m("20.00");
                info.setTps5m("20.00");
                info.setTps15m("20.00");
            }
        } catch (Exception e) {
            // 如果无法获取TPS，使用默认值
            info.setCurrentTps("20.00");
            info.setTps1m("20.00");
            info.setTps5m("20.00");
            info.setTps15m("20.00");
        }

        return info;
    }

    /**
     * 获取完整的服务器资源信息
     */
    public ServerResourceInfo getServerResourceInfo() {
        ServerResourceInfo info = new ServerResourceInfo();
        info.setMemory(getMemoryInfo());
        info.setCpu(getCpuInfo());
        info.setTps(getTpsInfo());

        // 添加服务器基本信息
        info.setServerName(Bukkit.getName());
        info.setServerVersion(Bukkit.getVersion());
        info.setBukkitVersion(Bukkit.getBukkitVersion());
        info.setOnlinePlayers(Bukkit.getOnlinePlayers().size());
        info.setMaxPlayers(Bukkit.getMaxPlayers());

        return info;
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return decimalFormat.format(bytes / 1024.0) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return decimalFormat.format(bytes / (1024.0 * 1024)) + " MB";
        } else {
            return decimalFormat.format(bytes / (1024.0 * 1024 * 1024)) + " GB";
        }
    }

    /**
     * 资源信息类
     */
    public static class ResourceInfo {
        private String type;
        private String used;
        private String max;
        private String committed;
        private String free;
        private String usagePercent;
        private String nonHeapUsed;
        private String nonHeapMax;
        private Integer availableProcessors;
        private String systemLoadAverage;
        private String osName;
        private String osVersion;
        private String osArch;
        private String currentTps;
        private String tps1m;
        private String tps5m;
        private String tps15m;

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getUsed() { return used; }
        public void setUsed(String used) { this.used = used; }
        public String getMax() { return max; }
        public void setMax(String max) { this.max = max; }
        public String getCommitted() { return committed; }
        public void setCommitted(String committed) { this.committed = committed; }
        public String getFree() { return free; }
        public void setFree(String free) { this.free = free; }
        public String getUsagePercent() { return usagePercent; }
        public void setUsagePercent(String usagePercent) { this.usagePercent = usagePercent; }
        public String getNonHeapUsed() { return nonHeapUsed; }
        public void setNonHeapUsed(String nonHeapUsed) { this.nonHeapUsed = nonHeapUsed; }
        public String getNonHeapMax() { return nonHeapMax; }
        public void setNonHeapMax(String nonHeapMax) { this.nonHeapMax = nonHeapMax; }
        public Integer getAvailableProcessors() { return availableProcessors; }
        public void setAvailableProcessors(Integer availableProcessors) { this.availableProcessors = availableProcessors; }
        public String getSystemLoadAverage() { return systemLoadAverage; }
        public void setSystemLoadAverage(String systemLoadAverage) { this.systemLoadAverage = systemLoadAverage; }
        public String getOsName() { return osName; }
        public void setOsName(String osName) { this.osName = osName; }
        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
        public String getOsArch() { return osArch; }
        public void setOsArch(String osArch) { this.osArch = osArch; }
        public String getCurrentTps() { return currentTps; }
        public void setCurrentTps(String currentTps) { this.currentTps = currentTps; }
        public String getTps1m() { return tps1m; }
        public void setTps1m(String tps1m) { this.tps1m = tps1m; }
        public String getTps5m() { return tps5m; }
        public void setTps5m(String tps5m) { this.tps5m = tps5m; }
        public String getTps15m() { return tps15m; }
        public void setTps15m(String tps15m) { this.tps15m = tps15m; }
    }

    /**
     * 服务器资源信息类
     */
    public static class ServerResourceInfo {
        private ResourceInfo memory;
        private ResourceInfo cpu;
        private ResourceInfo tps;
        private String serverName;
        private String serverVersion;
        private String bukkitVersion;
        private int onlinePlayers;
        private int maxPlayers;

        // Getters and Setters
        public ResourceInfo getMemory() { return memory; }
        public void setMemory(ResourceInfo memory) { this.memory = memory; }
        public ResourceInfo getCpu() { return cpu; }
        public void setCpu(ResourceInfo cpu) { this.cpu = cpu; }
        public ResourceInfo getTps() { return tps; }
        public void setTps(ResourceInfo tps) { this.tps = tps; }
        public String getServerName() { return serverName; }
        public void setServerName(String serverName) { this.serverName = serverName; }
        public String getServerVersion() { return serverVersion; }
        public void setServerVersion(String serverVersion) { this.serverVersion = serverVersion; }
        public String getBukkitVersion() { return bukkitVersion; }
        public void setBukkitVersion(String bukkitVersion) { this.bukkitVersion = bukkitVersion; }
        public int getOnlinePlayers() { return onlinePlayers; }
        public void setOnlinePlayers(int onlinePlayers) { this.onlinePlayers = onlinePlayers; }
        public int getMaxPlayers() { return maxPlayers; }
        public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    }
}