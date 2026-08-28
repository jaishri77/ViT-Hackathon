package com.sfl.aegisbackend;

import com.sfl.aegisbackend.model.Decision;
import com.sfl.aegisbackend.model.MarketData;
import com.sfl.aegisbackend.model.RiskAssessment;
import com.sfl.aegisbackend.model.TradeResult;

public class AegisResponse {

    private MarketData market;
    private RiskAssessment risk;
    private Decision decision;
    private TradeResult result;

    public AegisResponse() {
    }

    public AegisResponse(
            MarketData market,
            RiskAssessment risk,
            Decision decision,
            TradeResult result
    ) {
        this.market = market;
        this.risk = risk;
        this.decision = decision;
        this.result = result;
    }

    public MarketData getMarket() {
        return market;
    }

    public void setMarket(MarketData market) {
        this.market = market;
    }

    public RiskAssessment getRisk() {
        return risk;
    }

    public void setRisk(RiskAssessment risk) {
        this.risk = risk;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public TradeResult getResult() {
        return result;
    }

    public void setResult(TradeResult result) {
        this.result = result;
    }
}