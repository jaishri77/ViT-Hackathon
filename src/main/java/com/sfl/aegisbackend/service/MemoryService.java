package com.sfl.aegisbackend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MemoryService {

    private final List<String> memories = new ArrayList<>();

    private double currentExposure = 0.0;

    private double totalPnl = 0.0;


    // ==========================================
    // RECORD BUY
    // ==========================================

    public synchronized void recordBuy(
            double allocation,
            double pnl
    ) {

        if (allocation <= 0) {
            return;
        }

        currentExposure += allocation;

        totalPnl += pnl;

        memories.add(
                "ACTION: BUY"
                        + " | ALLOCATION: "
                        + allocation
                        + " | PNL: "
                        + pnl
        );
    }


    // ==========================================
    // RECORD SELL
    // ==========================================

    public synchronized void recordSell(
            double allocation,
            double pnl
    ) {

        if (allocation <= 0) {
            return;
        }

        currentExposure -= allocation;

        if (currentExposure < 0) {
            currentExposure = 0;
        }

        totalPnl += pnl;

        memories.add(
                "ACTION: SELL"
                        + " | ALLOCATION: "
                        + allocation
                        + " | PNL: "
                        + pnl
        );
    }


    // ==========================================
    // RECORD HOLD
    // ==========================================

    public synchronized void recordHold() {

        memories.add(
                "ACTION: HOLD"
        );
    }


    // ==========================================
    // GET CURRENT EXPOSURE
    // ==========================================

    public synchronized double getCurrentExposure() {

        return currentExposure;
    }


    // ==========================================
    // GET AVAILABLE CAPITAL
    // ==========================================

    public synchronized double getAvailableCapital() {

        double totalCapital = 100000.0;

        return Math.max(
                0,
                totalCapital - currentExposure
        );
    }


    // ==========================================
    // SAVE GENERAL MEMORY
    // ==========================================

    public synchronized void save(
            String memory
    ) {

        if (memory != null) {

            memories.add(memory);
        }
    }


    // ==========================================
    // GET MEMORIES
    // ==========================================

    public synchronized List<String> getMemories() {

        return new ArrayList<>(
                memories
        );
    }


    // ==========================================
    // TOTAL P&L
    // ==========================================

    public synchronized double getTotalPnl() {

        return totalPnl;
    }


    // ==========================================
    // ADD P&L
    // ==========================================

    public synchronized void addPnl(
            double pnl
    ) {

        totalPnl += pnl;
    }


    // ==========================================
    // CLEAR MEMORY
    // ==========================================

    public synchronized void clear() {

        memories.clear();

        currentExposure = 0.0;

        totalPnl = 0.0;
    }
}