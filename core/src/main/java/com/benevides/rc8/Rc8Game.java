package com.benevides.rc8;

import com.badlogic.gdx.Game;
import com.benevides.rc8.logic.SaveManager;
import com.benevides.rc8.screens.CutsceneScreen;

public class Rc8Game extends Game {
    private SaveManager saveManager;
    private SaveManager.SaveData saveData;

    @Override
    public void create() {
        saveManager = new SaveManager();
        saveData = saveManager.loadGame();
        setScreen(new CutsceneScreen(this, saveData.unlockedLevel, saveData.robotIntelligenceLevel));
    }

    public SaveManager getSaveManager() {
        return saveManager;
    }

    public SaveManager.SaveData getSaveData() {
        return saveData;
    }
}
