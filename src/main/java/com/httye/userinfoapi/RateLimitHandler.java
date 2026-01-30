package com.httye.userinfoapi;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

/**
 * API限流处理器
 * 防止API被滥用，支持按IP和API密钥限流
 */
public class RateLimitHandler implements HttpHandler {
    
    private final HttpHandler nextHandler;
    private final UserInfoAPIPlugin plugin;
    
    // 限流配置
    private final int requestsPerMinute;
    private final int requestsPerHour;
    private final boolean enabled;
    
    // 存储请求计数
    private final ConcurrentHashMap<String, RateLimitData> rateLimitMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    public RateLimitHandler(HttpHandler nextHandler, UserInfoAPIPlugin plugin) {
        this.nextHandler = nextHandler;
        this.plugin = plugin;
        
        // 从配置读取限流设置
        this.enabled = plugin.getConfig().getBoolean("rate-limit.enabled", true);
        this.requestsPerMinute = plugin.getConfig().getInt("rate-limit.requests-per-minute", 60);
        this.requestsPerHour = plugin.getConfig().getInt("rate-limit.requests-per-hour", 1000);
        
        // 启动定时清理任务
        startCleanupTask();
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!enabled) {
            nextHandler.handle(exchange);
            return;
        }
        
        String clientId = getClientIdentifier(exchange);
        RateLimitData rateLimitData = rateLimitMap.computeIfAbsent(clientId, k -> new RateLimitData());
        
        // 检查是否超过限流
        if (isRateLimited(rateLimitData)) {
            sendRateLimitResponse(exchange, rateLimitData);
            return;
        }
        
        // 记录请求
        rateLimitData.recordRequest();
        
        // 继续处理
        nextHandler.handle(exchange);
    }
    
    private String getClientIdentifier(HttpExchange exchange) {
        // 首先尝试获取API密钥
        String apiKey = extractApiKey(exchange);
        if (apiKey != null && !apiKey.isEmpty()) {
            return "key:" + apiKey;
        }
        
        // 否则使用IP地址
        String ipAddress = exchange.getRemoteAddress().getAddress().getHostAddress();
        return "ip:" + ipAddress;
    }
    
    private String extractApiKey(HttpExchange exchange) {
        // 从请求头获取
        String apiKey = exchange.getRequestHeaders().getFirst("X-API-Key");
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }
        
        // 从查询参数获取
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && keyValue[0].equals("api_key")) {
                    return keyValue[1];
                }
            }
        }
        
        return null;
    }
    
    private boolean isRateLimited(RateLimitData rateLimitData) {
        long now = System.currentTimeMillis();
        
        // 检查分钟限流
        int minuteRequests = rateLimitData.getRequestsInLastMinute(now);
        if (minuteRequests >= requestsPerMinute) {
            return true;
        }
        
        // 检查小时限流
        int hourRequests = rateLimitData.getRequestsInLastHour(now);
        if (hourRequests >= requestsPerHour) {
            return true;
        }
        
        return false;
    }
    
    private void sendRateLimitResponse(HttpExchange exchange, RateLimitData rateLimitData) throws IOException {
        long now = System.currentTimeMillis();
        int minuteRequests = rateLimitData.getRequestsInLastMinute(now);
        int hourRequests = rateLimitData.getRequestsInLastHour(now);
        
        long retryAfter = calculateRetryAfter(rateLimitData, now);
        
        String response = String.format(
            "{\"error\":\"请求过于频繁\",\"retryAfter\":%d,\"minuteRequests\":%d,\"hourRequests\":%d,\"requestsPerMinute\":%d,\"requestsPerHour\":%d}",
            retryAfter, minuteRequests, hourRequests, requestsPerMinute, requestsPerHour
        );
        
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("X-RateLimit-Limit-Minute", String.valueOf(requestsPerMinute));
        exchange.getResponseHeaders().add("X-RateLimit-Limit-Hour", String.valueOf(requestsPerHour));
        exchange.getResponseHeaders().add("X-RateLimit-Remaining-Minute", String.valueOf(Math.max(0, requestsPerMinute - minuteRequests)));
        exchange.getResponseHeaders().add("X-RateLimit-Remaining-Hour", String.valueOf(Math.max(0, requestsPerHour - hourRequests)));
        exchange.getResponseHeaders().add("Retry-After", String.valueOf(retryAfter));
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        
        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(429, bytes.length);
        
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private long calculateRetryAfter(RateLimitData rateLimitData, long now) {
        // 计算需要等待的时间（秒）
        long oldestMinuteRequest = rateLimitData.getOldestRequestInLastMinute(now);
        long oldestHourRequest = rateLimitData.getOldestRequestInLastHour(now);
        
        long minuteWait = Math.max(0, 60000 - (now - oldestMinuteRequest)) / 1000;
        long hourWait = Math.max(0, 3600000 - (now - oldestHourRequest)) / 1000;
        
        return Math.min(minuteWait, hourWait) + 1; // 加1秒确保限流重置
    }
    
    private void startCleanupTask() {
        // 每分钟清理一次过期的请求记录
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            rateLimitMap.entrySet().removeIf(entry -> {
                RateLimitData data = entry.getValue();
                return data.isExpired(now);
            });
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    public RateLimitStats getRateLimitStats(String clientId) {
        RateLimitData data = rateLimitMap.get(clientId);
        if (data == null) {
            return new RateLimitStats(0, 0, requestsPerMinute, requestsPerHour);
        }
        
        long now = System.currentTimeMillis();
        return new RateLimitStats(
            data.getRequestsInLastMinute(now),
            data.getRequestsInLastHour(now),
            requestsPerMinute,
            requestsPerHour
        );
    }
    
    /**
     * 限流数据类
     */
    private static class RateLimitData {
        private final List<Long> requestTimes = new ArrayList<>();
        private static final long ONE_HOUR = 3600000; // 1小时的毫秒数
        
        public synchronized void recordRequest() {
            requestTimes.add(System.currentTimeMillis());
            cleanupOldRequests();
        }
        
        public synchronized int getRequestsInLastMinute(long now) {
            cleanupOldRequests();
            long oneMinuteAgo = now - 60000;
            return (int) requestTimes.stream()
                    .filter(time -> time > oneMinuteAgo)
                    .count();
        }
        
        public synchronized int getRequestsInLastHour(long now) {
            cleanupOldRequests();
            long oneHourAgo = now - ONE_HOUR;
            return (int) requestTimes.stream()
                    .filter(time -> time > oneHourAgo)
                    .count();
        }
        
        public synchronized long getOldestRequestInLastMinute(long now) {
            cleanupOldRequests();
            long oneMinuteAgo = now - 60000;
            return requestTimes.stream()
                    .filter(time -> time > oneMinuteAgo)
                    .min(Long::compare)
                    .orElse(now);
        }
        
        public synchronized long getOldestRequestInLastHour(long now) {
            cleanupOldRequests();
            long oneHourAgo = now - ONE_HOUR;
            return requestTimes.stream()
                    .filter(time -> time > oneHourAgo)
                    .min(Long::compare)
                    .orElse(now);
        }
        
        public synchronized boolean isExpired(long now) {
            cleanupOldRequests();
            return requestTimes.isEmpty();
        }
        
        private synchronized void cleanupOldRequests() {
            long now = System.currentTimeMillis();
            long oneHourAgo = now - ONE_HOUR;
            requestTimes.removeIf(time -> time <= oneHourAgo);
        }
    }
    
    /**
     * 限流统计信息
     */
    public static class RateLimitStats {
        private final int minuteRequests;
        private final int hourRequests;
        private final int minuteLimit;
        private final int hourLimit;
        
        public RateLimitStats(int minuteRequests, int hourRequests, int minuteLimit, int hourLimit) {
            this.minuteRequests = minuteRequests;
            this.hourRequests = hourRequests;
            this.minuteLimit = minuteLimit;
            this.hourLimit = hourLimit;
        }
        
        public int getMinuteRequests() { return minuteRequests; }
        public int getHourRequests() { return hourRequests; }
        public int getMinuteLimit() { return minuteLimit; }
        public int getHourLimit() { return hourLimit; }
        public int getMinuteRemaining() { return Math.max(0, minuteLimit - minuteRequests); }
        public int getHourRemaining() { return Math.max(0, hourLimit - hourRequests); }
        public boolean isMinuteLimitReached() { return minuteRequests >= minuteLimit; }
        public boolean isHourLimitReached() { return hourRequests >= hourLimit; }
    }
}