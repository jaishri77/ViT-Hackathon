package com.sfl.aegisbackend.controller;

import com.sfl.aegisbackend.AegisResponse;
import com.sfl.aegisbackend.model.TradeResult;
import com.sfl.aegisbackend.service.AutonomousAgentService;
import com.sfl.aegisbackend.service.MemoryService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aegis")
@CrossOrigin(origins = "*")
public class AegisController {

    private final AutonomousAgentService autonomousAgent;
    private final MemoryService memory;

    public AegisController(
            AutonomousAgentService autonomousAgent,
            MemoryService memory
    ) {

        this.autonomousAgent = autonomousAgent;
        this.memory = memory;
    }

    // =====================================================
    // RUN AEGIS MANUALLY
    // =====================================================

    @GetMapping("/run")
    public AegisResponse run(
            @RequestParam(defaultValue = "RELIANCE")
            String symbol
    ) {

        return autonomousAgent.runCycle(symbol);
    }

    // =====================================================
    // GET LATEST AEGIS RESULT
    // =====================================================

    @GetMapping("/status")
    public AegisResponse status() {

        return autonomousAgent.getLatestResponse();
    }

    // =====================================================
    // GET MEMORY
    // =====================================================

    @GetMapping("/memory")
    public Object getMemory() {

        return memory.getMemories();
    }

    // =====================================================
    // GET CURRENT EXPOSURE
    // =====================================================

    @GetMapping("/exposure")
    public double getExposure() {

        return memory.getCurrentExposure();
    }

    // =====================================================
    // POST TRADE
    // =====================================================

    @PostMapping("/trade")
    public TradeResult saveTrade(
            @RequestBody TradeResult trade
    ) {

        // Only process executed trades
        if (!"EXECUTED".equalsIgnoreCase(
                trade.getStatus()
        )) {

            return trade;
        }

        // ==========================================
        // BUY
        // ==========================================

        if ("BUY".equalsIgnoreCase(
                trade.getAction()
        )) {

            memory.recordBuy(
                    trade.getAllocation(),
                    trade.getExecutionPrice()
            );
        }

        // ==========================================
        // SELL
        // ==========================================

        else if ("SELL".equalsIgnoreCase(
                trade.getAction()
        )) {

            memory.recordSell(
                    trade.getAllocation(),
                    trade.getExecutionPrice()
            );
        }

        return trade;
    }
}