package com.sfl.aegisbackend.service;

import com.sfl.aegisbackend.model.Decision;
import com.sfl.aegisbackend.model.MarketData;
import com.sfl.aegisbackend.model.RiskAssessment;

import org.springframework.stereotype.Service;

@Service
public class StrategistService {

    // Maximum exposure to one asset = 50% of capital
    private static final double MAX_EXPOSURE_RATIO = 0.50;

    // Start becoming conservative at 25%
    private static final double CAUTION_EXPOSURE_RATIO = 0.25;

    // Stop adding new exposure above 35%
    private static final double HIGH_EXPOSURE_RATIO = 0.35;

    public Decision decide(
            MarketData market,
            RiskAssessment risk
    ) {

        // ==========================================
        // 1. MARKET DATA
        // ==========================================

        double expectedReturn =
                market.getExpectedReturn();

        double newsSentiment =
                market.getNewsSentiment();

        double liquidity =
                market.getLiquidity();

        double volatility =
                market.getVolatility();

        double downsideRisk =
                market.getDownsideRisk();

        double spread =
                market.getSpread();

        double riskScore =
                risk.getRiskScore();

        double capital =
                market.getCapitalAvailable();

        double currentExposure =
                market.getCurrentExposure();

        // ==========================================
        // 2. EXPOSURE
        // ==========================================

        double exposureRatio = 0.0;

        if (capital > 0) {

            exposureRatio =
                    currentExposure / capital;
        }

        double maximumExposure =
                capital * MAX_EXPOSURE_RATIO;

        double exposureRoom =
                Math.max(
                        0.0,
                        maximumExposure - currentExposure
                );

        // ==========================================
        // 3. OPPORTUNITY SCORE
        // ==========================================

        double returnScore =
                normalize(
                        expectedReturn,
                        -0.01,
                        0.01
                );

        double sentimentScore =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                (newsSentiment + 1.0) / 2.0
                        )
                );

        double liquidityScore =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                liquidity
                        )
                );

        double volatilityScore =
                1.0 -
                        Math.min(
                                1.0,
                                volatility / 0.05
                        );

        double downsideScore =
                1.0 -
                        Math.min(
                                1.0,
                                downsideRisk / 0.05
                        );

        double spreadScore =
                1.0 -
                        Math.min(
                                1.0,
                                spread / 0.03
                        );

        double opportunityScore =
                returnScore * 0.25
                        + sentimentScore * 0.20
                        + liquidityScore * 0.15
                        + volatilityScore * 0.15
                        + downsideScore * 0.15
                        + spreadScore * 0.10;

        // ==========================================
        // 4. RISK-ADJUSTED SCORE
        // ==========================================

        double decisionScore =
                opportunityScore
                        * (1.0 - riskScore);

        // ==========================================
        // 5. NEGATIVE MARKET CONDITIONS
        // ==========================================

        boolean negativeMarket =
                expectedReturn < 0
                        || newsSentiment < -0.20
                        || downsideRisk > 0.03;

        boolean stronglyNegativeMarket =
                expectedReturn < -0.01
                        || newsSentiment < -0.40
                        || downsideRisk > 0.05;

        // ==========================================
        // 6. DETERMINE ACTION
        // ==========================================

        String action;

        /*
         * PRIORITY 1
         * ------------------------------
         * If we already have exposure and
         * the market becomes strongly negative,
         * reduce the position.
         */

        if (currentExposure > 0
                && stronglyNegativeMarket) {

            action = "SELL";
        }

        /*
         * PRIORITY 2
         * ------------------------------
         * High risk + negative market
         * = reduce exposure.
         */

        else if (
                currentExposure > 0
                        && riskScore >= 0.70
                        && negativeMarket
        ) {

            action = "SELL";
        }

        /*
         * PRIORITY 3
         * ------------------------------
         * Maximum exposure reached.
         */

        else if (
                exposureRatio >= MAX_EXPOSURE_RATIO
        ) {

            action = "HOLD";
        }

        /*
         * PRIORITY 4
         * ------------------------------
         * High exposure.
         *
         * Do not add more exposure.
         */

        else if (
                exposureRatio >= HIGH_EXPOSURE_RATIO
        ) {

            /*
             * If market is weakening, reduce exposure.
             * Otherwise simply HOLD.
             */

            if (
                    currentExposure > 0
                            && negativeMarket
            ) {

                action = "SELL";

            } else {

                action = "HOLD";
            }
        }

        /*
         * PRIORITY 5
         * ------------------------------
         * No capital available.
         */

        else if (exposureRoom <= 0) {

            action = "HOLD";
        }

        /*
         * PRIORITY 6
         * ------------------------------
         * Caution zone.
         *
         * Only allow BUY when opportunity
         * is very strong.
         */

        else if (
                exposureRatio >= CAUTION_EXPOSURE_RATIO
                        && (
                        decisionScore < 0.70
                                || expectedReturn <= 0
                )
        ) {

            action = "HOLD";
        }

        /*
         * PRIORITY 7
         * ------------------------------
         * Normal BUY condition.
         */

        else if (
                decisionScore >= 0.55
                        && expectedReturn > 0
                        && newsSentiment > -0.20
        ) {

            action = "BUY";
        }

        /*
         * PRIORITY 8
         * ------------------------------
         * Negative market with existing
         * exposure = SELL.
         */

        else if (
                currentExposure > 0
                        && negativeMarket
                        && decisionScore <= 0.45
        ) {

            action = "SELL";
        }

        /*
         * OTHERWISE
         */

        else {

            action = "HOLD";
        }

        // ==========================================
        // 7. CAPITAL ALLOCATION
        // ==========================================

        double allocation = 0.0;

        // ------------------------------------------
        // BUY
        // ------------------------------------------

        if (action.equals("BUY")) {

            double desiredAllocation =
                    risk.getMaxAllocation()
                            * decisionScore;

            allocation =
                    Math.min(
                            desiredAllocation,
                            exposureRoom
                    );
        }

        // ------------------------------------------
        // SELL
        // ------------------------------------------

        else if (action.equals("SELL")) {

            /*
             * Sell a portion of the current exposure.
             *
             * Strong negative conditions:
             *     sell up to 50%
             *
             * Normal negative conditions:
             *     sell up to 30%
             */

            double sellRatio;

            if (stronglyNegativeMarket) {

                sellRatio = 0.50;

            } else {

                sellRatio = 0.30;
            }

            double desiredSell =
                    currentExposure
                            * sellRatio;

            allocation =
                    Math.min(
                            desiredSell,
                            currentExposure
                    );
        }

        // ==========================================
        // 8. FINAL SAFETY CHECK
        // ==========================================

        allocation =
                Math.max(
                        0.0,
                        allocation
                );

        // ==========================================
        // 9. CONFIDENCE
        // ==========================================

        double confidence =
                calculateConfidence(
                        action,
                        decisionScore,
                        riskScore,
                        exposureRatio,
                        negativeMarket
                );

        // ==========================================
        // 10. REASONING
        // ==========================================

        String reasoning =
                generateReasoning(
                        action,
                        expectedReturn,
                        newsSentiment,
                        liquidity,
                        volatility,
                        downsideRisk,
                        spread,
                        riskScore,
                        decisionScore,
                        currentExposure,
                        maximumExposure,
                        exposureRatio,
                        negativeMarket,
                        stronglyNegativeMarket
                );

        // ==========================================
        // 11. RETURN DECISION
        // ==========================================

        return new Decision(
                action,
                round(allocation),
                reasoning,
                round(confidence)
        );
    }

    // ==========================================
    // NORMALIZE
    // ==========================================

    private double normalize(
            double value,
            double min,
            double max
    ) {

        double result =
                (value - min)
                        / (max - min);

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        result
                )
        );
    }

    // ==========================================
    // CONFIDENCE
    // ==========================================

    private double calculateConfidence(
            String action,
            double decisionScore,
            double riskScore,
            double exposureRatio,
            boolean negativeMarket
    ) {

        if (action.equals("BUY")) {

            return Math.min(
                    0.95,
                    0.50
                            + decisionScore * 0.35
                            + (1.0 - riskScore) * 0.10
                            + (1.0 - exposureRatio) * 0.05
            );
        }

        if (action.equals("SELL")) {

            double confidence =
                    0.55
                            + (1.0 - decisionScore) * 0.20
                            + riskScore * 0.15;

            if (negativeMarket) {

                confidence += 0.05;
            }

            return Math.min(
                    0.95,
                    confidence
            );
        }

        return Math.min(
                0.95,
                0.50
                        + riskScore * 0.20
                        + exposureRatio * 0.25
        );
    }

    // ==========================================
    // REASONING
    // ==========================================

    private String generateReasoning(
            String action,
            double expectedReturn,
            double newsSentiment,
            double liquidity,
            double volatility,
            double downsideRisk,
            double spread,
            double riskScore,
            double decisionScore,
            double currentExposure,
            double maximumExposure,
            double exposureRatio,
            boolean negativeMarket,
            boolean stronglyNegativeMarket
    ) {

        // ------------------------------------------
        // BUY REASONING
        // ------------------------------------------

        if (action.equals("BUY")) {

            return String.format(
                    "BUY selected because expected return is %.2f%%, "
                            + "news sentiment is %.2f, liquidity is %.2f "
                            + "and risk-adjusted opportunity score is %.2f. "
                            + "Current exposure is ₹%.2f of maximum ₹%.2f "
                            + "(%.1f%% exposure).",
                    expectedReturn * 100,
                    newsSentiment,
                    liquidity,
                    decisionScore,
                    currentExposure,
                    maximumExposure,
                    exposureRatio * 100
            );
        }

        // ------------------------------------------
        // SELL REASONING
        // ------------------------------------------

        if (action.equals("SELL")) {

            if (stronglyNegativeMarket) {

                return String.format(
                        "SELL selected to reduce exposure because market "
                                + "conditions are strongly negative. "
                                + "Expected return is %.2f%%, news sentiment "
                                + "is %.2f and downside risk is %.2f%%. "
                                + "Current exposure is ₹%.2f.",
                        expectedReturn * 100,
                        newsSentiment,
                        downsideRisk * 100,
                        currentExposure
                );
            }

            return String.format(
                    "SELL selected to reduce exposure because market "
                            + "conditions are weakening. Expected return "
                            + "is %.2f%%, news sentiment is %.2f and "
                            + "risk score is %.2f. Current exposure is ₹%.2f.",
                    expectedReturn * 100,
                    newsSentiment,
                    riskScore,
                    currentExposure
            );
        }

        // ------------------------------------------
        // MAXIMUM EXPOSURE
        // ------------------------------------------

        if (exposureRatio >= MAX_EXPOSURE_RATIO) {

            return String.format(
                    "HOLD selected because maximum exposure has been "
                            + "reached. Current exposure is ₹%.2f "
                            + "against maximum allowed ₹%.2f.",
                    currentExposure,
                    maximumExposure
            );
        }

        // ------------------------------------------
        // HIGH EXPOSURE
        // ------------------------------------------

        if (exposureRatio >= HIGH_EXPOSURE_RATIO) {

            return String.format(
                    "HOLD selected because exposure is high at %.1f%% "
                            + "of capital. Aegis is preventing additional "
                            + "BUY exposure.",
                    exposureRatio * 100
            );
        }

        // ------------------------------------------
        // CAUTION
        // ------------------------------------------

        if (exposureRatio >= CAUTION_EXPOSURE_RATIO) {

            return String.format(
                    "HOLD selected because portfolio exposure is already "
                            + "%.1f%%. Aegis requires a stronger "
                            + "risk-adjusted opportunity before adding "
                            + "another position.",
                    exposureRatio * 100
            );
        }

        // ------------------------------------------
        // HIGH RISK
        // ------------------------------------------

        if (riskScore >= 0.70) {

            return String.format(
                    "HOLD selected because risk is HIGH. "
                            + "Risk score is %.2f.",
                    riskScore
            );
        }

        // ------------------------------------------
        // DEFAULT HOLD
        // ------------------------------------------

        return String.format(
                "HOLD selected because the current opportunity does "
                        + "not provide sufficient risk-adjusted return. "
                        + "Risk score is %.2f, volatility is %.2f%%, "
                        + "downside risk is %.2f%%, liquidity is %.2f "
                        + "and current exposure is ₹%.2f.",
                riskScore,
                volatility * 100,
                downsideRisk * 100,
                liquidity,
                currentExposure
        );
    }

    // ==========================================
    // ROUND
    // ==========================================

    private double round(double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}