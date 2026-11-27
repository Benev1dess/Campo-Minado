package com.benevides.rc8.logic;

import com.benevides.rc8.model.ScoreEntry;
import java.util.ArrayList;
import java.util.List;

public class ScoreSorter {

    public static List<ScoreEntry> sortByEfficiency(List<ScoreEntry> entries) {
        if (entries == null || entries.size() <= 1) {
            return entries == null ? new ArrayList<>() : new ArrayList<>(entries);
        }

        List<ScoreEntry> copy = new ArrayList<>(entries);
        mergeSort(copy, 0, copy.size() - 1);
        return copy;
    }

    private static void mergeSort(List<ScoreEntry> list, int left, int right) {
        if (left >= right) {
            return;
        }

        int mid = left + (right - left) / 2;
        mergeSort(list, left, mid);
        mergeSort(list, mid + 1, right);
        merge(list, left, mid, right);
    }

    private static void merge(List<ScoreEntry> list, int left, int mid, int right) {
        List<ScoreEntry> temp = new ArrayList<>();
        int i = left;
        int j = mid + 1;

        while (i <= mid && j <= right) {
            // Vem primeiro quem tem maior pontuação; se empatar, menor tempo
            if (comesBefore(list.get(i), list.get(j))) {
                temp.add(list.get(i));
                i++;
            } else {
                temp.add(list.get(j));
                j++;
            }
        }

        while (i <= mid) {
            temp.add(list.get(i));
            i++;
        }

        while (j <= right) {
            temp.add(list.get(j));
            j++;
        }

        for (int k = 0; k < temp.size(); k++) {
            list.set(left + k, temp.get(k));
        }
    }

    private static boolean comesBefore(ScoreEntry a, ScoreEntry b) {
        if (a.getScore() != b.getScore()) {
            return a.getScore() > b.getScore();
        }
        return a.getTimeSeconds() < b.getTimeSeconds();
    }
}
