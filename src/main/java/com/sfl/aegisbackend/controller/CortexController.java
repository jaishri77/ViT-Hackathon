package com.aegis.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cortex")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CortexController {

    @GetMapping("/dashboard")
    public Map<String, String> getDashboard() {
        Map<String, String> data = new HashMap<>();
        data.put("portfolioValue", "$1,284,902.45");
        data.put("availableCapital", "$412,055.10");
        data.put("todayPnL", "+$14,202.12");
        data.put("portfolioExposure", "68.2%");
        data.put("riskScore", "2.4 / 10");
        data.put("symbol", "BTC / USDT");
        data.put("price", "$64,281.90");
        data.put("directive", "BUY");
        data.put("confidence", "94.2%");
        data.put("expectedReturn", "+4.8%");
        data.put("downside", "-1.4%");
        data.put("allocation", "25.0%");
        return data;
    }
}