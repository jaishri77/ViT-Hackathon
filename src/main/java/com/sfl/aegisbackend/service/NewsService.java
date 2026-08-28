package com.sfl.aegisbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfl.aegisbackend.model.NewsSentiment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Service
public class NewsService {

    @Value("${newsapi.api.key}")
    private String apiKey;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public NewsSentiment getSentiment(String symbol) {

        try {

            String query =
                    URLEncoder.encode(
                            symbol + " stock",
                            StandardCharsets.UTF_8
                    );

            String url =
                    "https://newsapi.org/v2/everything"
                            + "?q=" + query
                            + "&from=" + LocalDate.now().minusDays(2)
                            + "&language=en"
                            + "&sortBy=publishedAt"
                            + "&pageSize=10";

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("X-Api-Key", apiKey)
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonNode root =
                    objectMapper.readTree(response.body());

            JsonNode articles =
                    root.get("articles");

            if (articles == null || !articles.isArray()) {

                return new NewsSentiment(
                        0,
                        0,
                        "No recent news available"
                );
            }

            double totalScore = 0;
            int count = 0;

            for (JsonNode article : articles) {

                String title =
                        article.path("title")
                                .asText("");

                totalScore +=
                        calculateSentiment(title);

                count++;
            }

            double average =
                    count == 0
                            ? 0
                            : totalScore / count;

            String summary;

            if (average > 0.20) {

                summary = "Positive news sentiment";

            } else if (average < -0.20) {

                summary = "Negative news sentiment";

            } else {

                summary = "Neutral news sentiment";
            }

            return new NewsSentiment(
                    round(average),
                    count,
                    summary
            );

        } catch (Exception e) {

            return new NewsSentiment(
                    0,
                    0,
                    "News service unavailable"
            );
        }
    }

    private double calculateSentiment(String text) {

        String value =
                text.toLowerCase();

        String[] positiveWords = {
                "profit",
                "growth",
                "surge",
                "strong",
                "positive",
                "gain",
                "upgrade",
                "record",
                "success",
                "bullish"
        };

        String[] negativeWords = {
                "loss",
                "fall",
                "drop",
                "weak",
                "negative",
                "decline",
                "downgrade",
                "risk",
                "fraud",
                "bearish"
        };

        int score = 0;

        for (String word : positiveWords) {

            if (value.contains(word)) {
                score++;
            }
        }

        for (String word : negativeWords) {

            if (value.contains(word)) {
                score--;
            }
        }

        if (score > 0) {
            return 1.0;
        }

        if (score < 0) {
            return -1.0;
        }

        return 0;
    }

    private double round(double value) {

        return Math.round(value * 100.0)
                / 100.0;
    }
}