package com.sfl.aegisbackend.controller;

import org.springframework.web.bind.annotation.*;
        import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cortex")
@CrossOrigin(origins = "*") // Allows your Node server on port 3000 to talk to Spring Boot on port 8080
public class CortexController {

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        // Portfolio Stats
        data.put("portfolioValue", "$1,284,902.45");
        data.put("availableCapital", "$412,055.10");
        data.put("todayPnL", "+$14,202.12");
        data.put("portfolioExposure", "68.2%");
        data.put("riskScore", "2.4 / 10");

        // Market Price Data
        data.put("symbol", "BTC / USDT");
        data.put("price", "$64,281.90");

        // AI Model Directive
        data.put("directive", "BUY");
        data.put("confidence", "94.2%");
        data.put("expectedReturn", "+4.8%");
        data.put("downside", "-1.4%");
        data.put("allocation", "25.0%");
        data.put("decisionId", "DEC-942-X881");

        return data;
    }

    @PostMapping("/execute")
    public Map<String, String> executeTradeDirective(@RequestBody(required = false) Map<String, Object> payload) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Trade directive executed successfully!");
        return response;
    }
}