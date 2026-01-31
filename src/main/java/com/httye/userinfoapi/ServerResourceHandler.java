package com.httye.userinfoapi;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 服务器资源监控API处理器
 */
public class ServerResourceHandler implements HttpHandler {

    private final UserInfoAPIPlugin plugin;
    private final APIServer apiServer;
    private final Gson gson;
    private final ServerResourceMonitor monitor;

    public ServerResourceHandler() {
        this.plugin = UserInfoAPIPlugin.getInstance();
        this.apiServer = plugin.getApiServer();
        this.gson = plugin.getGson();
        this.monitor = new ServerResourceMonitor();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long startTime = System.currentTimeMillis();

        try {
            String query = exchange.getRequestURI().getQuery();
            String type = getQueryParam(query, "type");

            JsonObject response = new JsonObject();

            if (type == null || type.isEmpty() || type.equals("all")) {
                // 返回所有资源信息
                ServerResourceMonitor.ServerResourceInfo resourceInfo = monitor.getServerResourceInfo();
                response.add("data", gson.toJsonTree(resourceInfo));
                response.addProperty("type", "all");
            } else if (type.equals("memory")) {
                // 只返回内存信息
                ServerResourceMonitor.ResourceInfo memoryInfo = monitor.getMemoryInfo();
                response.add("data", gson.toJsonTree(memoryInfo));
                response.addProperty("type", "memory");
            } else if (type.equals("cpu")) {
                // 只返回CPU信息
                ServerResourceMonitor.ResourceInfo cpuInfo = monitor.getCpuInfo();
                response.add("data", gson.toJsonTree(cpuInfo));
                response.addProperty("type", "cpu");
            } else if (type.equals("tps")) {
                // 只返回TPS信息
                ServerResourceMonitor.ResourceInfo tpsInfo = monitor.getTpsInfo();
                response.add("data", gson.toJsonTree(tpsInfo));
                response.addProperty("type", "tps");
            } else {
                sendErrorResponse(exchange, 400, "无效的类型参数，支持: all, memory, cpu, tps");
                return;
            }

            // 添加元数据
            response.addProperty("timestamp", System.currentTimeMillis());
            response.addProperty("plugin", "UserInfoAPI");
            response.addProperty("version", "2.0");

            long responseTime = System.currentTimeMillis() - startTime;
            response.addProperty("responseTime", responseTime + "ms");

            sendResponse(exchange, 200, response.toString());

            if (apiServer != null) {
                apiServer.recordRequest(true, responseTime);
            }

        } catch (Exception e) {
            e.printStackTrace();

            long responseTime = System.currentTimeMillis() - startTime;

            if (apiServer != null) {
                apiServer.recordRequest(false, responseTime);
            }

            sendErrorResponse(exchange, 500, "处理请求时发生错误: " + e.getMessage());
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, X-API-Key");

        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        sendResponse(exchange, statusCode, error.toString());
    }

    private String getQueryParam(String query, String paramName) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                return keyValue[1];
            }
        }
        return null;
    }
}