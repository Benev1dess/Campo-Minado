package com.benevides.rc8.logic;

import com.benevides.rc8.model.InventorySlot;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager<T> {
    private final List<InventorySlot<T>> slots;

    public ModuleManager() {
        this.slots = new ArrayList<>();
    }

    public void addSlot(InventorySlot<T> slot) {
        if (slot != null) {
            slots.add(slot);
        }
    }

    public boolean unlockSlot(String slotId) {
        for (InventorySlot<T> slot : slots) {
            if (slot.getSlotId().equals(slotId)) {
                slot.setUnlocked(true);
                return true;
            }
        }
        return false;
    }

    public List<InventorySlot<T>> getUnlockedSlots() {
        List<InventorySlot<T>> unlocked = new ArrayList<>();
        for (InventorySlot<T> slot : slots) {
            if (slot.isUnlocked()) {
                unlocked.add(slot);
            }
        }
        return unlocked;
    }

    public int countUnlocked() {
        int count = 0;
        for (InventorySlot<T> slot : slots) {
            if (slot.isUnlocked()) {
                count++;
            }
        }
        return count;
    }

    public List<InventorySlot<T>> getAllSlots() {
        return slots;
    }

    // Exemplo de método genérico com limite Comparable para pegar o menor elemento de uma lista
    public static <E extends Comparable<E>> E findMin(List<E> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        E min = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i).compareTo(min) < 0) {
                min = values.get(i);
            }
        }
        return min;
    }
}
