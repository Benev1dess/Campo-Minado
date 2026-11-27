package com.benevides.rc8.model;

public class ScoreEntry implements Comparable<ScoreEntry> {
    private String actorName; // Ex.: "Jogador" ou "RC-8"
    private int level;
    private int score;
    private float timeSeconds;

    public ScoreEntry(String actorName, int level, int score, float timeSeconds) {
        this.actorName = actorName;
        this.level = level;
        this.score = score;
        this.timeSeconds = timeSeconds;
    }

    public String getActorName() {
        return actorName;
    }

    public int getLevel() {
        return level;
    }

    public int getScore() {
        return score;
    }

    public float getTimeSeconds() {
        return timeSeconds;
    }

    // Ordena por maior pontuação; em caso de empate, pelo menor tempo
    @Override
    public int compareTo(ScoreEntry other) {
        if (this.score != other.score) {
            return Integer.compare(other.score, this.score);
        }
        return Float.compare(this.timeSeconds, other.timeSeconds);
    }

    @Override
    public String toString() {
        return actorName + " | Fase " + level + " | Pts: " + score + " | " + String.format("%.1fs", timeSeconds);
    }
}
