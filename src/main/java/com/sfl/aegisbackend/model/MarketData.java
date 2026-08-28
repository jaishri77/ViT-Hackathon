package com.sfl.aegisbackend.model;

public class MarketData {

    private String symbol;
    private double price;
    private double volume;
    private double volatility;
    private double liquidity;
    private double sentiment;
    private double spread;

    private double expectedReturn;
    private double downsideRisk;
    private double capitalAvailable;
    private double currentExposure;
    private double newsSentiment;

    private String dataTimestamp;

    public MarketData() {
    }

    public MarketData(
            String symbol,
            double price,
            double volume,
            double volatility,
            double liquidity,
            double sentiment,
            double spread
    ) {
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.volatility = volatility;
        this.liquidity = liquidity;
        this.sentiment = sentiment;
        this.spread = spread;
    }

    public MarketData(
            String symbol,
            double price,
            double volume,
            double volatility,
            double liquidity,
            double sentiment,
            double spread,
            double expectedReturn,
            double downsideRisk,
            double capitalAvailable,
            double currentExposure,
            double newsSentiment,
            String dataTimestamp
    ) {
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.volatility = volatility;
        this.liquidity = liquidity;
        this.sentiment = sentiment;
        this.spread = spread;
        this.expectedReturn = expectedReturn;
        this.downsideRisk = downsideRisk;
        this.capitalAvailable = capitalAvailable;
        this.currentExposure = currentExposure;
        this.newsSentiment = newsSentiment;
        this.dataTimestamp = dataTimestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getVolatility() {
        return volatility;
    }

    public void setVolatility(double volatility) {
        this.volatility = volatility;
    }

    public double getLiquidity() {
        return liquidity;
    }

    public void setLiquidity(double liquidity) {
        this.liquidity = liquidity;
    }

    public double getSentiment() {
        return sentiment;
    }

    public void setSentiment(double sentiment) {
        this.sentiment = sentiment;
    }

    public double getSpread() {
        return spread;
    }

    public void setSpread(double spread) {
        this.spread = spread;
    }

    public double getExpectedReturn() {
        return expectedReturn;
    }

    public void setExpectedReturn(double expectedReturn) {
        this.expectedReturn = expectedReturn;
    }

    public double getDownsideRisk() {
        return downsideRisk;
    }

    public void setDownsideRisk(double downsideRisk) {
        this.downsideRisk = downsideRisk;
    }

    public double getCapitalAvailable() {
        return capitalAvailable;
    }

    public void setCapitalAvailable(double capitalAvailable) {
        this.capitalAvailable = capitalAvailable;
    }

    public double getCurrentExposure() {
        return currentExposure;
    }

    public void setCurrentExposure(double currentExposure) {
        this.currentExposure = currentExposure;
    }

    public double getNewsSentiment() {
        return newsSentiment;
    }

    public void setNewsSentiment(double newsSentiment) {
        this.newsSentiment = newsSentiment;
    }

    public String getDataTimestamp() {
        return dataTimestamp;
    }

    public void setDataTimestamp(String dataTimestamp) {
        this.dataTimestamp = dataTimestamp;
    }
}