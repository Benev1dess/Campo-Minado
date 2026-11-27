package com.benevides.rc8.model;

public class Cell {
    private boolean hasMine;       // Tem mina ou não?
    private boolean isOpen;        // Já foi aberta pelo jogador/robô?
    private boolean isFlagged;     // Tem bandeira colocada?
    private int neighborMines;     // Quantas minas existem ao redor (0 a 8)

    public Cell() {
        this.hasMine = false;
        this.isOpen = false;
        this.isFlagged = false;
        this.neighborMines = 0;
    }

    // Getters e Setters fundamentais
    public boolean isHasMine() { return hasMine; }
    public void setHasMine(boolean hasMine) { this.hasMine = hasMine; }

    public boolean isOpen() { return isOpen; }
    public void setOpen(boolean open) { isOpen = open; }

    public boolean isFlagged() { return isFlagged; }
    public void setFlagged(boolean flagged) { isFlagged = flagged; }

    public int getNeighborMines() { return neighborMines; }
    public void setNeighborMines(int neighborMines) { this.neighborMines = neighborMines; }
}
