package com.sfl.aegisbackend.model;

public class RiskAssessment {

    private double riskScore;
    private String riskLevel;
    private double maxAllocation;
    private double estimatedSlippage;

    public RiskAssessment() {
    }

    public RiskAssessment(
            double riskScore,
            String riskLevel,
            double maxAllocation,
            double estimatedSlippage
    ) {
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.maxAllocation = maxAllocation;
        this.estimatedSlippage = estimatedSlippage;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getMaxAllocation() {
        return maxAllocation;
    }

    public void setMaxAllocation(double maxAllocation) {
        this.maxAllocation = maxAllocation;
    }

    public double getEstimatedSlippage() {
        return estimatedSlippage;
    }

    public void setEstimatedSlippage(double estimatedSlippage) {
        this.estimatedSlippage = estimatedSlippage;
    }
}