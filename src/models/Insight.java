package models;

import java.time.LocalDate;

public class Insight {
    private int insightId;
    private LocalDate date;
    private int totalTime;
    private int productiveTime;
    private int score;
    private String notes;

    public Insight(int insightId, LocalDate date, int totalTime, int productiveTime, int score, String notes) {
        this.insightId = insightId;
        this.date = date;
        this.totalTime = totalTime;
        this.productiveTime = productiveTime;
        this.score = score;
        this.notes = notes;
    }

    public int getInsightId() { return insightId; }
    public void setInsightId(int insightId) { this.insightId = insightId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getTotalTime() { return totalTime; }
    public void setTotalTime(int totalTime) { this.totalTime = totalTime; }

    public int getProductiveTime() { return productiveTime; }
    public void setProductiveTime(int productiveTime) { this.productiveTime = productiveTime; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
