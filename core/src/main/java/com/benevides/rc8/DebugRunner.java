package com.benevides.rc8;

import com.benevides.rc8.logic.ActionHistoryList;
import com.benevides.rc8.logic.ModuleManager;
import com.benevides.rc8.logic.ScoreSorter;
import com.benevides.rc8.model.InventorySlot;
import com.benevides.rc8.model.LogicModule;
import com.benevides.rc8.model.RobotAction;
import com.benevides.rc8.model.ScoreEntry;
import java.util.ArrayList;
import java.util.List;

public class DebugRunner {
    public static void main(String[] args) {
        // 1. Testando Generics
        ModuleManager<LogicModule> modules = new ModuleManager<>();
        modules.addSlot(new InventorySlot<>("M1", new LogicModule("Detector de Bandeira", 1, "Marca minas obvias"), true));
        modules.addSlot(new InventorySlot<>("M2", new LogicModule("Abertura Segura", 2, "Abre vizinhos livres"), false));
        System.out.println("Modulos desbloqueados: " + modules.countUnlocked());

        // 2. Testando Lista Encadeada
        ActionHistoryList history = new ActionHistoryList();
        history.addAction(new RobotAction(1, 2, RobotAction.ActionType.FLAG, "Teste 1"));
        history.addAction(new RobotAction(1, 3, RobotAction.ActionType.REVEAL, "Teste 2"));
        while (!history.isEmpty()) {
            System.out.println("Passo: " + history.pollFirst());
        }

        // 3. Testando Algoritmo de Ordenacao (Merge Sort)
        List<ScoreEntry> scores = new ArrayList<>();
        scores.add(new ScoreEntry("RC-8", 1, 120, 18.5f));
        scores.add(new ScoreEntry("Jogador", 1, 250, 22.0f));
        scores.add(new ScoreEntry("RC-8", 2, 250, 14.2f));

        List<ScoreEntry> ordered = ScoreSorter.sortByEfficiency(scores);
        System.out.println("--- Ranking Ordenado ---");
        for (ScoreEntry entry : ordered) {
            System.out.println(entry);
        }
    }
}
