package com.benevides.rc8.logic;

import com.benevides.rc8.model.Cell;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Board {
    private int rows;
    private int cols;
    private Cell[][] grid;

    public static class Coord {
        public int r;
        public int c;

        public Coord(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }

    public Board(int rows, int cols, int totalMines) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new Cell[rows][cols];

        initializeBoard();
        placeMines(totalMines);
        calculateNeighbors();
    }

    private void initializeBoard() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell();
            }
        }
    }

    private void placeMines(int totalMines) {
        Random rand = new Random();
        int placed = 0;

        while (placed < totalMines) {
            int r = rand.nextInt(rows);
            int c = rand.nextInt(cols);

            if (!grid[r][c].isHasMine()) {
                grid[r][c].setHasMine(true);
                placed++;
            }
        }
    }

    private void calculateNeighbors() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].isHasMine()) {
                    continue;
                }

                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) {
                            continue;
                        }

                        int nr = r + dr;
                        int nc = c + dc;

                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                            if (grid[nr][nc].isHasMine()) {
                                count++;
                            }
                        }
                    }
                }
                grid[r][c].setNeighborMines(count);
            }
        }
    }

    public void openCell(int startR, int startC) {
        if (startR < 0 || startR >= rows || startC < 0 || startC >= cols) {
            return;
        }

        Cell clicked = grid[startR][startC];
        if (clicked.isOpen() || clicked.isFlagged()) {
            return;
        }

        if (clicked.isHasMine()) {
            clicked.setOpen(true);
            return;
        }

        Queue<Coord> queue = new LinkedList<>();
        clicked.setOpen(true);
        queue.add(new Coord(startR, startC));

        while (!queue.isEmpty()) {
            Coord current = queue.poll();
            Cell currentCell = grid[current.r][current.c];

            if (currentCell.getNeighborMines() == 0) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) {
                            continue;
                        }

                        int nr = current.r + dr;
                        int nc = current.c + dc;

                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                            Cell neighbor = grid[nr][nc];
                            if (!neighbor.isOpen() && !neighbor.isFlagged() && !neighbor.isHasMine()) {
                                neighbor.setOpen(true);
                                if (neighbor.getNeighborMines() == 0) {
                                    queue.add(new Coord(nr, nc));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public Cell getCell(int r, int c) {
        return grid[r][c];
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }
}
