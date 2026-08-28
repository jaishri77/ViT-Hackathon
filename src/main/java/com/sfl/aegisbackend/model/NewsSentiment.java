package com.sfl.aegisbackend.model;

public class NewsSentiment {

    private double score;
    private int articleCount;
    private String summary;

    public NewsSentiment() {
    }

    public NewsSentiment(
            double score,
            int articleCount,
            String summary
    ) {
        this.score = score;
        this.articleCount = articleCount;
        this.summary = summary;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(int articleCount) {
        this.articleCount = articleCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}