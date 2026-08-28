package com.sfl.aegisbackend.service;

import com.sfl.aegisbackend.AegisResponse;
import com.sfl.aegisbackend.model.Decision;
import com.sfl.aegisbackend.model.MarketData;
import com.sfl.aegisbackend.model.RiskAssessment;
import com.sfl.aegisbackend.model.TradeResult;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AutonomousAgentService {

    private final ObserverService observer;
    private final QuantService quant;
    private final StrategistService strategist;
    private final ExecutorService executor;
    private final MemoryService memory;

    private String lastProcessedMarketTime = null;

    // Stores the latest complete AEGIS result
    private AegisResponse latestResponse = null;

    public AutonomousAgentService(
            ObserverService observer,
            QuantService quant,
            StrategistService strategist,
            ExecutorService executor,
            MemoryService memory
    ) {

        this.observer = observer;
        this.quant = quant;
        this.strategist = strategist;
        this.executor = executor;
        this.memory = memory;
    }

    // =====================================================
    // AUTONOMOUS CYCLE
    // =====================================================

    @Scheduled(fixedDelay = 30000)
    public void autonomousCycle() {

        runCycle("RELIANCE");
    }

    // =====================================================
    // MANUAL / API CYCLE
    // =====================================================

    public AegisResponse runCycle(String symbol) {

        try {

            System.out.println();
            System.out.println(
                    "========== AEGIS AUTONOMOUS CYCLE =========="
            );

            // ==========================================
            // 1. OBSERVE
            // ==========================================

            MarketData market =
                    observer.observe(symbol);

            String currentMarketTime =
                    market.getDataTimestamp();

            System.out.println(
                    "PRICE: "
                            + market.getPrice()
            );

            System.out.println(
                    "MARKET TIME: "
                            + currentMarketTime
            );

            // ==========================================
            // 2. DUPLICATE CANDLE PROTECTION
            // ==========================================

            if (currentMarketTime != null
                    && currentMarketTime.equals(
                    lastProcessedMarketTime
            )) {

                System.out.println(
                        "STATUS: SAME MARKET CANDLE"
                );

                System.out.println(
                        "ACTION: SKIPPED"
                );

                System.out.println(
                        "REASON: No new market data available."
                );

                System.out.println(
                        "============================================"
                );

                return latestResponse;
            }

            // ==========================================
            // 3. MARK CANDLE AS PROCESSED
            // ==========================================

            lastProcessedMarketTime =
                    currentMarketTime;

            // ==========================================
            // 4. QUANT
            // ==========================================

            RiskAssessment risk =
                    quant.evaluate(market);

            System.out.println(
                    "RISK: "
                            + risk.getRiskLevel()
            );

            System.out.println(
                    "RISK SCORE: "
                            + risk.getRiskScore()
            );

            // ==========================================
            // 5. STRATEGIST
            // ==========================================

            Decision decision =
                    strategist.decide(
                            market,
                            risk
                    );

            System.out.println(
                    "DECISION: "
                            + decision.getAction()
            );

            System.out.println(
                    "CONFIDENCE: "
                            + decision.getConfidence()
            );

            System.out.println(
                    "ALLOCATION: ₹"
                            + decision.getAllocation()
            );

            // ==========================================
            // 6. EXECUTOR
            // ==========================================

            TradeResult result =
                    executor.execute(
                            market,
                            decision,
                            risk
                    );

            System.out.println(
                    "EXECUTION: "
                            + result.getStatus()
            );

            // ==========================================
            // 7. MEMORY
            // ==========================================

            if ("EXECUTED".equals(
                    result.getStatus()
            )) {

                if ("BUY".equals(
                        result.getAction()
                )) {

                    memory.recordBuy(
                            result.getAllocation(),
                            result.getExecutionPrice()
                    );

                } else if ("SELL".equals(
                        result.getAction()
                )) {

                    memory.recordSell(
                            result.getAllocation(),
                            result.getExecutionPrice()
                    );
                }
            }

            // ==========================================
            // 8. EXPOSURE
            // ==========================================

            double exposure =
                    memory.getCurrentExposure();

            System.out.println(
                    "EXPOSURE: ₹"
                            + round(exposure)
            );

            // ==========================================
            // 9. P&L
            // ==========================================

            System.out.println(
                    "P&L: ₹"
                            + round(result.getPnl())
            );

            System.out.println(
                    "============================================"
            );

            // ==========================================
            // 10. CREATE RESPONSE
            // ==========================================

            latestResponse =
                    new AegisResponse(
                            market,
                            risk,
                            decision,
                            result
                    );

            return latestResponse;

        } catch (Exception e) {

            System.out.println();
            System.out.println(
                    "AEGIS CYCLE FAILED: "
                            + e.getMessage()
            );

            System.out.println(
                    "============================================"
            );

            throw new RuntimeException(
                    "AEGIS cycle failed",
                    e
            );
        }
    }

    // =====================================================
    // GET LATEST RESULT
    // =====================================================

    public AegisResponse getLatestResponse() {

        return latestResponse;
    }

    // =====================================================
    // ROUND
    // =====================================================

    private double round(double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}