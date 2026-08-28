package com.sfl.aegisbackend.model;

public class Decision {

    private String action;
    private double allocation;
    private String reasoning;
    private double confidence;

    public Decision() {
    }

    public Decision(
            String action,
            double allocation,
            String reasoning
    ) {
        this.action = action;
        this.allocation = allocation;
        this.reasoning = reasoning;
        this.confidence = 0.0;
    }

    public Decision(
            String action,
            double allocation,
            String reasoning,
            double confidence
    ) {
        this.action = action;
        this.allocation = allocation;
        this.reasoning = reasoning;
        this.confidence = confidence;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public double getAllocation() {
        return allocation;
    }

    public void setAllocation(double allocation) {
        this.allocation = allocation;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}