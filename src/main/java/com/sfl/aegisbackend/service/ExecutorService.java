package com.sfl.aegisbackend.service;

import com.sfl.aegisbackend.model.Decision;
import com.sfl.aegisbackend.model.MarketData;
import com.sfl.aegisbackend.model.RiskAssessment;
import com.sfl.aegisbackend.model.TradeResult;

import org.springframework.stereotype.Service;

@Service
public class ExecutorService {

    private final MemoryService memory;

    public ExecutorService(
            MemoryService memory
    ) {

        this.memory = memory;
    }

    public TradeResult execute(
            MarketData market,
            Decision decision,
            RiskAssessment risk
    ) {

        // =====================================
        // 1. HOLD
        // =====================================

        if (decision.getAction()
                .equals("HOLD")) {

            return new TradeResult(
                    market.getSymbol(),
                    "HOLD",
                    0,
                    market.getPrice(),
                    0,
                    0,
                    "NO TRADE"
            );
        }

        // =====================================
        // 2. CHECK ALLOCATION
        // =====================================

        if (decision.getAllocation()
                <= 0) {

            return new TradeResult(
                    market.getSymbol(),
                    "BLOCKED",
                    0,
                    market.getPrice(),
                    0,
                    0,
                    "INVALID ALLOCATION"
            );
        }

        // =====================================
        // 3. CHECK RISK LIMIT
        // =====================================

        if (decision.getAllocation()
                > risk.getMaxAllocation()) {

            return new TradeResult(
                    market.getSymbol(),
                    "BLOCKED",
                    0,
                    market.getPrice(),
                    0,
                    0,
                    "RISK LIMIT EXCEEDED"
            );
        }

        // =====================================
        // 4. CHECK AVAILABLE CAPITAL
        // =====================================

        if (decision.getAction()
                .equals("BUY")
                && decision.getAllocation()
                > memory.getAvailableCapital()) {

            return new TradeResult(
                    market.getSymbol(),
                    "BLOCKED",
                    0,
                    market.getPrice(),
                    0,
                    0,
                    "INSUFFICIENT CAPITAL"
            );
        }

        // =====================================
        // 5. EXECUTION PRICE
        // =====================================

        double executionPrice;

        if (decision.getAction()
                .equals("BUY")) {

            executionPrice =
                    market.getPrice()
                            + risk.getEstimatedSlippage();

        } else {

            executionPrice =
                    market.getPrice()
                            - risk.getEstimatedSlippage();
        }

        // =====================================
        // 6. SIMULATED RETURN
        // =====================================

        double simulatedReturn =
                market.getSentiment()
                        * 0.02;

        double pnl =
                decision.getAllocation()
                        * simulatedReturn;

        // =====================================
        // 7. UPDATE POSITION
        // =====================================

        if (decision.getAction()
                .equals("BUY")) {

            memory.recordBuy(
                    decision.getAllocation(),
                    pnl
            );

        } else if (decision.getAction()
                .equals("SELL")) {

            double sellAmount =
                    Math.min(
                            decision.getAllocation(),
                            memory.getCurrentExposure()
                    );

            if (sellAmount <= 0) {

                return new TradeResult(
                        market.getSymbol(),
                        "BLOCKED",
                        0,
                        market.getPrice(),
                        0,
                        0,
                        "NO POSITION TO SELL"
                );
            }

            memory.recordSell(
                    sellAmount,
                    pnl
            );
        }

        // =====================================
        // 8. RETURN RESULT
        // =====================================

        return new TradeResult(
                market.getSymbol(),
                decision.getAction(),
                decision.getAllocation(),
                round(executionPrice),
                risk.getEstimatedSlippage(),
                round(pnl),
                "EXECUTED"
        );
    }

    private double round(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}