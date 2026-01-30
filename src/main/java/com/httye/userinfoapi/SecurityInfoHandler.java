package com.httye.userinfoapi;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class SecurityInfoHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        UserInfoAPIPlugin plugin = UserInfoAPIPlugin.getInstance();
        JsonObject securityInfo = plugin.getSecurityManager().getSecurityInfo();
        
        sendResponse(exchange, 200, securityInfo.toString());
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        
        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}