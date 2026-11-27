package com.benevides.rc8.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.benevides.rc8.model.ScoreEntry;
import java.util.ArrayList;
import java.util.List;

public class SaveManager {
    private static final String SAVE_FILE_PATH = "rc8_save_data.txt";

    public static class SaveData {
        public int unlockedLevel = 1;
        public int robotIntelligenceLevel = 1;
        public List<ScoreEntry> scores = new ArrayList<>();
    }

    private FileHandle getSaveFile() {
        // Gdx.files.local salva na pasta local do aplicativo/projeto de forma multiplataforma
        return Gdx.files.local(SAVE_FILE_PATH);
    }

    public void saveGame(SaveData data) {
        if (data == null || Gdx.files == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("unlockedLevel=").append(data.unlockedLevel).append("\n");
        sb.append("robotIntelligenceLevel=").append(data.robotIntelligenceLevel).append("\n");

        for (ScoreEntry entry : data.scores) {
            // Formato: SCORE|actorName|level|score|timeSeconds
            sb.append("SCORE|")
                .append(sanitize(entry.getActorName())).append("|")
                .append(entry.getLevel()).append("|")
                .append(entry.getScore()).append("|")
                .append(entry.getTimeSeconds())
                .append("\n");
        }

        FileHandle file = getSaveFile();
        file.writeString(sb.toString(), false);
    }

    public SaveData loadGame() {
        SaveData data = new SaveData();
        if (Gdx.files == null) {
            return data;
        }

        FileHandle file = getSaveFile();
        if (!file.exists()) {
            return data;
        }

        String content = file.readString();
        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            if (line.startsWith("unlockedLevel=")) {
                data.unlockedLevel = parseIntSafe(line.substring("unlockedLevel=".length()), 1);
            } else if (line.startsWith("robotIntelligenceLevel=")) {
                data.robotIntelligenceLevel = parseIntSafe(line.substring("robotIntelligenceLevel=".length()), 1);
            } else if (line.startsWith("SCORE|")) {
                String[] parts = line.split("\\|");
                if (parts.length == 5) {
                    String actor = parts[1];
                    int level = parseIntSafe(parts[2], 1);
                    int score = parseIntSafe(parts[3], 0);
                    float time = parseFloatSafe(parts[4], 0f);
                    data.scores.add(new ScoreEntry(actor, level, score, time));
                }
            }
        }

        return data;
    }

    private String sanitize(String value) {
        if (value == null) return "RC-8";
        return value.replace("|", "/").replace("\n", " ").replace("\r", " ");
    }

    private int parseIntSafe(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private float parseFloatSafe(String raw, float fallback) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
