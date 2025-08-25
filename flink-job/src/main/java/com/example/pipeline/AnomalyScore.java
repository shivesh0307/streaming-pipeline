package com.example.pipeline;

public class AnomalyScore {
    public Event event;
    public boolean anomaly;
    public double score;

    public AnomalyScore() {
    }

    public AnomalyScore(Event e, boolean a, double s) {
        this.event = e;
        this.anomaly = a;
        this.score = s;
    }
}