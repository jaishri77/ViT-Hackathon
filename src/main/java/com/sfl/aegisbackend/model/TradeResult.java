package com.sfl.aegisbackend.model;

public class TradeResult {

    private String symbol;
    private String action;
    private double allocation;
    private double executionPrice;
    private double slippage;
    private double pnl;
    private String status;

    public TradeResult() {
    }

    public TradeResult(
            String symbol,
            String action,
            double allocation,
            double executionPrice,
            double slippage,
            double pnl,
            String status
    ) {
        this.symbol = symbol;
        this.action = action;
        this.allocation = allocation;
        this.executionPrice = executionPrice;
        this.slippage = slippage;
        this.pnl = pnl;
        this.status = status;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getAction() {
        return action;
    }

    public double getAllocation() {
        return allocation;
    }

    public double getExecutionPrice() {
        return executionPrice;
    }

    public double getSlippage() {
        return slippage;
    }

    public double getPnl() {
        return pnl;
    }

    public String getStatus() {
        return status;
    }
}