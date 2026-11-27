package com.benevides.rc8.logic;

import com.benevides.rc8.model.Cell;
import com.benevides.rc8.model.RobotAction;

public class Rc8Brain {
    private int intelligenceLevel; // Ex.: 1 = básico (apenas bandeiras), 2+ = bandeiras + abertura segura

    public Rc8Brain(int intelligenceLevel) {
        this.intelligenceLevel = Math.max(1, intelligenceLevel);
    }

    public int getIntelligenceLevel() {
        return intelligenceLevel;
    }

    public void setIntelligenceLevel(int intelligenceLevel) {
        this.intelligenceLevel = Math.max(1, intelligenceLevel);
    }

    /**
     * Busca no tabuleiro por uma dedução válida e registra a ação na lista encadeada.
     * Retorna a ação encontrada, ou null se não houver dedução direta disponível.
     */
    public RobotAction planNextMove(Board board, ActionHistoryList history) {
        int rows = board.getRows();
        int cols = board.getCols();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = board.getCell(r, c);
                if (!cell.isOpen() || cell.getNeighborMines() <= 0) {
                    continue;
                }

                int neededMines = cell.getNeighborMines();
                int flaggedCount = 0;
                int closedUnflaggedCount = 0;

                int firstFlagR = -1, firstFlagC = -1;
                int firstSafeR = -1, firstSafeC = -1;

                // Busca nos 8 vizinhos da célula aberta
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;

                        int nr = r + dr;
                        int nc = c + dc;
                        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;

                        Cell neighbor = board.getCell(nr, nc);
                        if (neighbor.isFlagged()) {
                            flaggedCount++;
                        } else if (!neighbor.isOpen()) {
                            closedUnflaggedCount++;
                            if (firstFlagR == -1) {
                                firstFlagR = nr;
                                firstFlagC = nc;
                            }
                            if (firstSafeR == -1) {
                                firstSafeR = nr;
                                firstSafeC = nc;
                            }
                        }
                    }
                }

                int totalClosedNeighbors = flaggedCount + closedUnflaggedCount;

                // Regra 1: Todas as fechadas ao redor precisam ser minas
                if (totalClosedNeighbors == neededMines && closedUnflaggedCount > 0) {
                    RobotAction action = new RobotAction(
                        firstFlagR,
                        firstFlagC,
                        RobotAction.ActionType.FLAG,
                        "Célula (" + r + "," + c + ") exige " + neededMines + " mina(s)"
                    );
                    history.addAction(action);
                    return action;
                }

                // Regra 2 (Nível 2+): Minas já marcadas; restantes são seguras
                if (intelligenceLevel >= 2 && flaggedCount == neededMines && closedUnflaggedCount > 0) {
                    RobotAction action = new RobotAction(
                        firstSafeR,
                        firstSafeC,
                        RobotAction.ActionType.REVEAL,
                        "Minas de (" + r + "," + c + ") já marcadas; célula segura"
                    );
                    history.addAction(action);
                    return action;
                }
            }
        }

        return null;
    }
}
