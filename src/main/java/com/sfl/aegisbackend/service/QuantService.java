package com.sfl.aegisbackend.service;

import com.sfl.aegisbackend.model.MarketData;
import com.sfl.aegisbackend.model.RiskAssessment;

import org.springframework.stereotype.Service;

@Service
public class QuantService {

    public RiskAssessment evaluate(MarketData market) {

        // ==============================
        // MARKET FACTORS
        // ==============================

        double volatility = market.getVolatility();
        double liquidity = market.getLiquidity();
        double spread = market.getSpread();

        double capital = market.getCapitalAvailable();
        double exposure = market.getCurrentExposure();

        double expectedReturn = market.getExpectedReturn();
        double downsideRisk = market.getDownsideRisk();

        // ==============================
        // NORMALIZE RISK FACTORS
        // ==============================

        // Higher volatility = higher risk
        double volatilityRisk =
                Math.min(1.0, volatility / 0.05);

        // Lower liquidity = higher risk
        double liquidityRisk =
                1.0 - Math.min(1.0, liquidity);

        // Higher spread = higher risk
        double spreadRisk =
                Math.min(1.0, spread / 0.03);

        // No capital = maximum risk
        double capitalRisk =
                capital <= 0 ? 1.0 : 0.0;

        // Exposure relative to capital
        double exposureRatio =
                capital <= 0
                        ? 1.0
                        : exposure / capital;

        double exposureRisk =
                Math.min(1.0, exposureRatio);

        // Negative expected return = risk
        double returnRisk =
                expectedReturn < 0
                        ? Math.min(
                        1.0,
                        Math.abs(expectedReturn) / 0.03
                )
                        : 0.0;

        // Higher downside = higher risk
        double downside =
                Math.min(
                        1.0,
                        downsideRisk / 0.03
                );

        // ==============================
        // COMBINED RISK SCORE
        // ==============================

        double riskScore =
                volatilityRisk * 0.20
                        + liquidityRisk * 0.10
                        + spreadRisk * 0.10
                        + capitalRisk * 0.10
                        + exposureRisk * 0.15
                        + returnRisk * 0.15
                        + downside * 0.20;

        riskScore =
                Math.max(
                        0.0,
                        Math.min(1.0, riskScore)
                );

        // ==============================
        // RISK LEVEL
        // ==============================

        String riskLevel;

        if (riskScore < 0.30) {

            riskLevel = "LOW";

        } else if (riskScore < 0.60) {

            riskLevel = "MEDIUM";

        } else {

            riskLevel = "HIGH";
        }

        // ==============================
        // AVAILABLE CAPITAL
        // ==============================

        double availableCapital =
                Math.max(
                        0.0,
                        capital - exposure
                );

        // ==============================
        // RISK-ADJUSTED ALLOCATION
        // ==============================

        /*
         * Maximum portfolio allocation for one trade.
         *
         * LOW risk    -> up to 20%
         * MEDIUM risk -> up to 10%
         * HIGH risk   -> up to 5%
         */

        double allocationLimit;

        if (riskScore < 0.30) {

            allocationLimit =
                    availableCapital * 0.20;

        } else if (riskScore < 0.60) {

            allocationLimit =
                    availableCapital * 0.10;

        } else {

            allocationLimit =
                    availableCapital * 0.05;
        }

        // Further reduce allocation according to risk
        double riskMultiplier =
                1.0 - (riskScore * 0.50);

        double maxAllocation =
                allocationLimit * riskMultiplier;

        // Never allocate more than available capital
        maxAllocation =
                Math.min(
                        maxAllocation,
                        availableCapital
                );

        // ==============================
        // ESTIMATED SLIPPAGE
        // ==============================

        double estimatedSlippage =
                spread
                        * (
                        1.0
                                + volatility
                                + (1.0 - liquidity)
                );

        // ==============================
        // RETURN RESULT
        // ==============================

        return new RiskAssessment(
                round(riskScore),
                riskLevel,
                round(maxAllocation),
                round(estimatedSlippage)
        );
    }

    private double round(double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}