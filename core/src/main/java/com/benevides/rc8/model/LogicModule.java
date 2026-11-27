package com.benevides.rc8.model;

public class LogicModule {
    private String name;
    private int requiredLevel;
    private String description;

    public LogicModule(String name, int requiredLevel, String description) {
        this.name = name;
        this.requiredLevel = requiredLevel;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name + " (Nv. " + requiredLevel + "): " + description;
    }
}
