package com.sfl.aegisbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfl.aegisbackend.model.MarketData;
import com.sfl.aegisbackend.model.NewsSentiment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class ObserverService {

    @Value("${aegis.capital:100000}")
    private double capital;

    private final NewsService newsService;
    private final MemoryService memoryService;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public ObserverService(
            NewsService newsService,
            MemoryService memoryService
    ) {
        this.newsService = newsService;
        this.memoryService = memoryService;
    }

    public MarketData observe(String symbol) {

        try {

            // ==========================================
            // 1. YAHOO SYMBOL
            // ==========================================

            String yahooSymbol =
                    symbol.toUpperCase() + ".NS";

            String encodedSymbol =
                    URLEncoder.encode(
                            yahooSymbol,
                            StandardCharsets.UTF_8
                    );

            // ==========================================
            // 2. GET MARKET DATA
            // ==========================================

            String url =
                    "https://query1.finance.yahoo.com/v8/finance/chart/"
                            + encodedSymbol
                            + "?interval=5m"
                            + "&range=5d";

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header(
                                    "User-Agent",
                                    "Mozilla/5.0"
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {

                throw new RuntimeException(
                        "Yahoo Finance returned HTTP "
                                + response.statusCode()
                );
            }

            // ==========================================
            // 3. PARSE RESPONSE
            // ==========================================

            JsonNode root =
                    objectMapper.readTree(
                            response.body()
                    );

            JsonNode result =
                    root.path("chart")
                            .path("result")
                            .get(0);

            if (result == null
                    || result.isMissingNode()) {

                throw new RuntimeException(
                        "No market data available for "
                                + symbol
                );
            }

            JsonNode quote =
                    result.path("indicators")
                            .path("quote")
                            .get(0);

            if (quote == null
                    || quote.isMissingNode()) {

                throw new RuntimeException(
                        "Invalid Yahoo quote data"
                );
            }

            // ==========================================
            // 4. GET ARRAYS
            // ==========================================

            JsonNode timestamps =
                    result.path("timestamp");

            JsonNode closes =
                    quote.path("close");

            JsonNode highs =
                    quote.path("high");

            JsonNode lows =
                    quote.path("low");

            JsonNode volumes =
                    quote.path("volume");

            // ==========================================
            // 5. FIND LATEST VALID PRICE
            // ==========================================

            int latestIndex =
                    findLatestValidIndex(closes);

            if (latestIndex < 0) {

                throw new RuntimeException(
                        "No valid price found for "
                                + symbol
                );
            }

            double price =
                    closes.get(latestIndex)
                            .asDouble();

            // ==========================================
            // 6. PREVIOUS PRICE
            // ==========================================

            int previousIndex =
                    findPreviousValidIndex(
                            closes,
                            latestIndex
                    );

            double previousPrice =
                    closes.get(previousIndex)
                            .asDouble();

            // ==========================================
            // 7. EXPECTED RETURN
            // ==========================================

            double expectedReturn = 0;

            if (previousPrice > 0) {

                expectedReturn =
                        (price - previousPrice)
                                / previousPrice;
            }

            // ==========================================
            // 8. VOLATILITY
            // ==========================================

            double volatility =
                    calculateVolatility(closes);

            // ==========================================
            // 9. DOWNSIDE RISK
            // ==========================================

            double downsideRisk =
                    calculateDownsideRisk(closes);

            // ==========================================
            // 10. HIGH / LOW
            // ==========================================

            double high =
                    getValidValue(
                            highs,
                            latestIndex,
                            price
                    );

            double low =
                    getValidValue(
                            lows,
                            latestIndex,
                            price
                    );

            // ==========================================
            // 11. SPREAD
            // ==========================================

            double spread = 0;

            if (price > 0 && high >= low) {

                spread =
                        (high - low)
                                / price;
            }

            // ==========================================
            // 12. VOLUME
            // ==========================================

            double volume =
                    getLatestVolume(
                            volumes,
                            latestIndex
                    );

            // ==========================================
            // 13. LIQUIDITY
            // ==========================================

            double liquidity =
                    calculateLiquidity(
                            volumes,
                            latestIndex
                    );

            // ==========================================
            // 14. NEWS SENTIMENT
            // ==========================================

            NewsSentiment news =
                    newsService.getSentiment(symbol);

            double newsScore =
                    news.getScore();

            // ==========================================
            // 15. COMBINED SENTIMENT
            // ==========================================

            double combinedSentiment =
                    (newsScore * 0.6)
                            + (expectedReturn * 0.4);

            // ==========================================
            // 16. CURRENT EXPOSURE
            // ==========================================

            double currentExposure =
                    memoryService.getCurrentExposure();

            // ==========================================
            // 17. ACTUAL MARKET TIMESTAMP
            // ==========================================

            String marketTimestamp =
                    getMarketTimestamp(
                            timestamps,
                            latestIndex
                    );

            // ==========================================
            // 18. CREATE MARKET DATA
            // ==========================================

            return new MarketData(
                    symbol,
                    round(price, 2),
                    round(volume, 0),
                    round(volatility, 6),
                    round(liquidity, 4),
                    round(combinedSentiment, 4),
                    round(spread, 6),
                    round(expectedReturn, 6),
                    round(downsideRisk, 6),
                    capital,
                    currentExposure,
                    round(newsScore, 4),
                    marketTimestamp
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to observe market data: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =====================================================
    // MARKET TIMESTAMP
    // =====================================================

    private String getMarketTimestamp(
            JsonNode timestamps,
            int latestIndex
    ) {

        if (timestamps != null
                && timestamps.isArray()
                && latestIndex >= 0
                && latestIndex < timestamps.size()) {

            JsonNode timestampNode =
                    timestamps.get(latestIndex);

            if (timestampNode != null
                    && timestampNode.isNumber()) {

                long epochSeconds =
                        timestampNode.asLong();

                return Instant.ofEpochSecond(
                        epochSeconds
                ).toString();
            }
        }

        return Instant.now().toString();
    }

    // =====================================================
    // FIND LATEST VALID PRICE
    // =====================================================

    private int findLatestValidIndex(
            JsonNode closes
    ) {

        for (int i = closes.size() - 1;
             i >= 0;
             i--) {

            if (isValidNumber(closes.get(i))) {

                return i;
            }
        }

        return -1;
    }

    // =====================================================
    // FIND PREVIOUS VALID PRICE
    // =====================================================

    private int findPreviousValidIndex(
            JsonNode closes,
            int currentIndex
    ) {

        for (int i = currentIndex - 1;
             i >= 0;
             i--) {

            if (isValidNumber(closes.get(i))) {

                return i;
            }
        }

        return currentIndex;
    }

    // =====================================================
    // VOLATILITY
    // =====================================================

    private double calculateVolatility(
            JsonNode closes
    ) {

        double sum = 0;

        int count = 0;

        for (int i = 1;
             i < closes.size();
             i++) {

            if (!isValidNumber(closes.get(i))
                    || !isValidNumber(
                    closes.get(i - 1))) {

                continue;
            }

            double current =
                    closes.get(i).asDouble();

            double previous =
                    closes.get(i - 1).asDouble();

            if (previous <= 0) {
                continue;
            }

            double returnValue =
                    (current - previous)
                            / previous;

            sum +=
                    returnValue
                            * returnValue;

            count++;
        }

        if (count == 0) {
            return 0;
        }

        return Math.sqrt(
                sum / count
        );
    }

    // =====================================================
    // DOWNSIDE RISK
    // =====================================================

    private double calculateDownsideRisk(
            JsonNode closes
    ) {

        double downside = 0;

        int count = 0;

        for (int i = 1;
             i < closes.size();
             i++) {

            if (!isValidNumber(closes.get(i))
                    || !isValidNumber(
                    closes.get(i - 1))) {

                continue;
            }

            double current =
                    closes.get(i).asDouble();

            double previous =
                    closes.get(i - 1).asDouble();

            if (previous <= 0) {
                continue;
            }

            double returnValue =
                    (current - previous)
                            / previous;

            if (returnValue < 0) {

                downside +=
                        Math.abs(returnValue);

                count++;
            }
        }

        if (count == 0) {
            return 0;
        }

        return downside / count;
    }

    // =====================================================
    // LIQUIDITY
    // =====================================================

    private double calculateLiquidity(
            JsonNode volumes,
            int latestIndex
    ) {

        double latestVolume =
                getLatestVolume(
                        volumes,
                        latestIndex
                );

        if (latestVolume <= 0) {
            return 0;
        }

        double totalVolume = 0;

        int count = 0;

        int start =
                Math.max(
                        0,
                        latestIndex - 30
                );

        for (int i = start;
             i <= latestIndex;
             i++) {

            if (!isValidNumber(
                    volumes.get(i))) {

                continue;
            }

            double currentVolume =
                    volumes.get(i).asDouble();

            if (currentVolume > 0) {

                totalVolume += currentVolume;

                count++;
            }
        }

        if (count == 0) {
            return 0;
        }

        double averageVolume =
                totalVolume / count;

        if (averageVolume <= 0) {
            return 0;
        }

        double ratio =
                latestVolume
                        / averageVolume;

        return Math.min(
                1.0,
                ratio / 2.0
        );
    }

    // =====================================================
    // GET LATEST VOLUME
    // =====================================================

    private double getLatestVolume(
            JsonNode volumes,
            int latestIndex
    ) {

        if (latestIndex < volumes.size()
                && isValidNumber(
                volumes.get(latestIndex))) {

            double volume =
                    volumes.get(latestIndex)
                            .asDouble();

            if (volume > 0) {
                return volume;
            }
        }

        for (int i = latestIndex - 1;
             i >= 0;
             i--) {

            if (isValidNumber(
                    volumes.get(i))) {

                double volume =
                        volumes.get(i)
                                .asDouble();

                if (volume > 0) {
                    return volume;
                }
            }
        }

        return 0;
    }

    // =====================================================
    // GET HIGH / LOW SAFELY
    // =====================================================

    private double getValidValue(
            JsonNode values,
            int index,
            double fallback
    ) {

        if (index >= 0
                && index < values.size()
                && isValidNumber(
                values.get(index))) {

            double value =
                    values.get(index)
                            .asDouble();

            if (value > 0) {
                return value;
            }
        }

        return fallback;
    }

    // =====================================================
    // VALID NUMBER
    // =====================================================

    private boolean isValidNumber(
            JsonNode node
    ) {

        return node != null
                && !node.isNull()
                && node.isNumber()
                && node.asDouble() > 0;
    }

    // =====================================================
    // ROUND
    // =====================================================

    private double round(
            double value,
            int decimals
    ) {

        double multiplier =
                Math.pow(
                        10,
                        decimals
                );

        return Math.round(
                value * multiplier
        ) / multiplier;
    }
}