package com.benevides.rc8.model;

public class RobotAction {
    public enum ActionType {
        REVEAL,
        FLAG
    }

    private int row;
    private int col;
    private ActionType type;
    private String reason;

    public RobotAction(int row, int col, ActionType type, String reason) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.reason = reason;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public ActionType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "[" + type + "] (" + row + ", " + col + ") - " + reason;
    }
}
