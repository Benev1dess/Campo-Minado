package com.benevides.rc8.model;

public class InventorySlot<T> {
    private String slotId;
    private T item;
    private boolean unlocked;

    public InventorySlot(String slotId, T item, boolean unlocked) {
        this.slotId = slotId;
        this.item = item;
        this.unlocked = unlocked;
    }

    public String getSlotId() {
        return slotId;
    }

    public T getItem() {
        return item;
    }

    public void setItem(T item) {
        this.item = item;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    @Override
    public String toString() {
        return "[" + slotId + "] " + (unlocked ? "UNLOCKED" : "LOCKED") + " -> " + item;
    }
}
