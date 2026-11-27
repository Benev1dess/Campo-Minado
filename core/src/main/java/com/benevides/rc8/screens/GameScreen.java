package com.benevides.rc8.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.benevides.rc8.Rc8Game;
import com.benevides.rc8.logic.ActionHistoryList;
import com.benevides.rc8.logic.Board;
import com.benevides.rc8.logic.Rc8Brain;
import com.benevides.rc8.logic.SaveManager;
import com.benevides.rc8.logic.ScoreSorter;
import com.benevides.rc8.model.Cell;
import com.benevides.rc8.model.RobotAction;
import com.benevides.rc8.model.ScoreEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameScreen extends ScreenAdapter {

    public enum GameState {
        PLAYING,
        GAME_OVER,
        VICTORY
    }

    private enum RobotAnimPhase {
        IDLE,
        MOVING,
        ACTING
    }

    private static class Particle {
        float x, y, vx, vy, life, maxLife, size;
        Color color;

        Particle(float x, float y, float vx, float vy, float size, Color color) {
            this(x, y, vx, vy, size, color, 0.45f);
        }

        Particle(float x, float y, float vx, float vy, float size, Color color, float life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.life = life;
            this.maxLife = life;
        }
    }

    /** Onda de choque em forma de anel que se expande. */
    private static class Ring {
        float x, y, t, duration, maxRadius, thickness;
        Color color;

        Ring(float x, float y, float duration, float maxRadius, float thickness, Color color) {
            this.x = x;
            this.y = y;
            this.duration = duration;
            this.maxRadius = maxRadius;
            this.thickness = thickness;
            this.color = color;
        }
    }

    /** Explosao secundaria (reacao em cadeia) das outras minas. */
    private static class Blast {
        float x, y, delay;

        Blast(float x, float y, float delay) {
            this.x = x;
            this.y = y;
            this.delay = delay;
        }
    }

    private static final float EXPLOSION_DURATION = 2.4f;

    private final Rc8Game game;
    private final int level;

    private Board board;
    private Rc8Brain rc8Brain;
    private ActionHistoryList history;
    private GameState gameState;

    private float elapsedTime;
    private int lastScore;
    private String lastRobotMessage = "RC-8 pronto. [ESPACO] = animacao do robo | [N] = novo tabuleiro";
    private String statusBanner = "";

    private RobotAnimPhase robotPhase = RobotAnimPhase.IDLE;
    private RobotAction pendingAction = null;
    private float robotX;
    private float robotY;
    private float phaseTimer = 0f;
    private float animTime = 0f;
    private final List<Particle> particles = new ArrayList<>();

    // Explosao em tela cheia
    private boolean explosionActive = false;
    private float explosionTime = 0f;
    private float explosionX;
    private float explosionY;
    private final List<Ring> rings = new ArrayList<>();
    private final List<Blast> blasts = new ArrayList<>();
    private final Matrix4 viewMatrix = new Matrix4();

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private final int cellSize = 46;
    private final int margin = 3;
    private final float boardOffsetX = 40f;
    private final float boardOffsetY = 65f;

    public GameScreen(Rc8Game game, int level, int robotLevel) {
        this.game = game;
        this.level = level;
        this.rc8Brain = new Rc8Brain(Math.max(2, robotLevel));
        this.history = new ActionHistoryList();
        resetBoard();
    }

    private void resetBoard() {
        int rows = 8 + Math.min(level, 6);
        int cols = 8 + Math.min(level, 6);
        int mines = 8 + (level * 3);

        this.board = new Board(rows, cols, mines);
        this.history.clear();
        this.particles.clear();
        this.rings.clear();
        this.blasts.clear();
        this.explosionActive = false;
        this.explosionTime = 0f;
        this.gameState = GameState.PLAYING;
        this.elapsedTime = 0f;
        this.lastScore = 0;
        this.statusBanner = "";

        this.robotPhase = RobotAnimPhase.IDLE;
        this.pendingAction = null;
        this.robotX = boardOffsetX - 14f;
        this.robotY = getCurrentBoardOffsetY() + (rows * (cellSize + margin)) * 0.5f;
        this.lastRobotMessage = "Novo tabuleiro (" + rows + "x" + cols + ", " + mines + " minas)";
    }

    private float getCurrentBoardOffsetY() {
        float h = Gdx.graphics.getHeight();
        float gridH = board.getRows() * (cellSize + margin) + margin;
        float safeTopLimit = h - 95f;
        return Math.min(boardOffsetY, safeTopLimit - gridH);
    }

    private float cellCenterX(int col) {
        return boardOffsetX + col * (cellSize + margin) + cellSize * 0.5f;
    }

    private float cellCenterY(int row) {
        return getCurrentBoardOffsetY() + row * (cellSize + margin) + cellSize * 0.5f;
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.2f);
    }

    private void startRobotMove(RobotAction move) {
        this.pendingAction = move;
        this.robotPhase = RobotAnimPhase.MOVING;
        this.phaseTimer = 0f;
        this.lastRobotMessage = "RC-8 indo para (" + move.getRow() + ", " + move.getCol() + ") [" + move.getType() + "]...";
    }

    private void spawnBreakParticles(float cx, float cy, Color baseColor) {
        for (int i = 0; i < 10; i++) {
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float speed = MathUtils.random(45f, 140f);
            float vx = MathUtils.cos(angle) * speed;
            float vy = MathUtils.sin(angle) * speed;
            particles.add(new Particle(cx, cy, vx, vy, MathUtils.random(4f, 8f), baseColor));
        }
    }

    // ------------------------------------------------------------------
    // EXPLOSAO
    // ------------------------------------------------------------------

    private void startExplosion(int hitRow, int hitCol) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float maxR = (float) Math.hypot(w, h);

        explosionActive = true;
        explosionTime = 0f;
        explosionX = cellCenterX(hitCol);
        explosionY = cellCenterY(hitRow);

        // Ondas de choque
        rings.add(new Ring(explosionX, explosionY, 0.8f, maxR, 14f, Color.WHITE));
        rings.add(new Ring(explosionX, explosionY, 1.2f, maxR * 0.9f, 10f, Color.ORANGE));
        rings.add(new Ring(explosionX, explosionY, 1.6f, maxR * 0.75f, 7f, Color.FIREBRICK));

        // Fagulhas e estilhacos espalhados pela tela
        Color[] palette = {
            Color.ORANGE, Color.GOLD, Color.RED, Color.FIREBRICK, Color.DARK_GRAY, Color.WHITE
        };
        for (int i = 0; i < 170; i++) {
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float speed = MathUtils.random(250f, 1100f);
            float vx = MathUtils.cos(angle) * speed;
            float vy = MathUtils.sin(angle) * speed;
            Color c = palette[MathUtils.random(palette.length - 1)];
            particles.add(new Particle(
                explosionX, explosionY, vx, vy,
                MathUtils.random(3f, 11f), c, MathUtils.random(0.9f, 1.9f)));
        }

        // Reacao em cadeia: as outras minas explodem em seguida
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if ((r == hitRow && c == hitCol) || !board.getCell(r, c).isHasMine()) {
                    continue;
                }
                blasts.add(new Blast(cellCenterX(c), cellCenterY(r), MathUtils.random(0.2f, 1.3f)));
            }
        }
    }

    private void updateExplosion(float delta) {
        if (explosionActive) {
            explosionTime += delta;
            if (explosionTime >= EXPLOSION_DURATION) {
                explosionActive = false;
            }
        }

        Iterator<Blast> bit = blasts.iterator();
        while (bit.hasNext()) {
            Blast b = bit.next();
            b.delay -= delta;
            if (b.delay <= 0f) {
                spawnBreakParticles(b.x, b.y, Color.ORANGE);
                spawnBreakParticles(b.x, b.y, Color.GOLD);
                rings.add(new Ring(b.x, b.y, 0.5f, 80f, 5f, Color.ORANGE));
                bit.remove();
            }
        }

        Iterator<Ring> rit = rings.iterator();
        while (rit.hasNext()) {
            Ring ring = rit.next();
            ring.t += delta;
            if (ring.t >= ring.duration) {
                rit.remove();
            }
        }
    }

    private void drawRing(float cx, float cy, float radius, float thickness) {
        int segments = 64;
        float prevX = cx + radius;
        float prevY = cy;
        for (int i = 1; i <= segments; i++) {
            float a = i * MathUtils.PI2 / segments;
            float nx = cx + MathUtils.cos(a) * radius;
            float ny = cy + MathUtils.sin(a) * radius;
            shapeRenderer.rectLine(prevX, prevY, nx, ny, thickness);
            prevX = nx;
            prevY = ny;
        }
    }

    private void drawFireball(float w, float h) {
        if (!explosionActive) {
            return;
        }

        float t = explosionTime;
        float maxR = (float) Math.hypot(w, h);

        // Crescimento rapido que desacelera
        float k = 1f - MathUtils.clamp(t / 0.8f, 0f, 1f);
        float grow = 1f - k * k * k;
        float radius = maxR * grow;
        float fade = MathUtils.clamp(1f - (t - 0.45f) / 1.2f, 0f, 1f);

        if (fade > 0f) {
            shapeRenderer.setColor(0.85f, 0.12f, 0.05f, 0.92f * fade);
            shapeRenderer.circle(explosionX, explosionY, radius, 96);
            shapeRenderer.setColor(1f, 0.45f, 0.05f, 0.92f * fade);
            shapeRenderer.circle(explosionX, explosionY, radius * 0.78f, 96);
            shapeRenderer.setColor(1f, 0.80f, 0.20f, 0.95f * fade);
            shapeRenderer.circle(explosionX, explosionY, radius * 0.50f, 96);
            shapeRenderer.setColor(1f, 0.97f, 0.80f, fade);
            shapeRenderer.circle(explosionX, explosionY, radius * 0.25f, 96);
        }

        // Fumaca escura que sobe e some
        float smokeT = MathUtils.clamp((t - 0.4f) / 2.0f, 0f, 1f);
        float smokeAlpha = 0.55f * MathUtils.sin(MathUtils.PI * smokeT);
        if (smokeAlpha > 0f) {
            shapeRenderer.setColor(0.05f, 0.05f, 0.06f, smokeAlpha);
            shapeRenderer.rect(-60f, -60f, w + 120f, h + 120f);
        }
    }

    private void drawRings() {
        for (Ring ring : rings) {
            float p = ring.t / ring.duration;
            float inv = 1f - p;
            float eased = 1f - inv * inv;
            shapeRenderer.setColor(ring.color.r, ring.color.g, ring.color.b, inv);
            drawRing(ring.x, ring.y, ring.maxRadius * eased, ring.thickness * inv + 2f);
        }
    }

    private void drawFlash(float w, float h) {
        if (!explosionActive) {
            return;
        }
        float a = MathUtils.clamp(1f - explosionTime / 0.35f, 0f, 1f);
        if (a > 0f) {
            shapeRenderer.setColor(1f, 1f, 1f, a);
            shapeRenderer.rect(-60f, -60f, w + 120f, h + 120f);
        }
    }

    // ------------------------------------------------------------------
    // ROBO
    // ------------------------------------------------------------------

    private void drawCuteRc8(float x, float y, float scale, Color accentColor) {
        float bob = MathUtils.sin(animTime * 6f) * 3f * scale;
        float cy = y + bob;

        // Sombra no chao
        shapeRenderer.setColor(0f, 0f, 0f, 0.35f);
        shapeRenderer.circle(x, y - 18f * scale, 16f * scale);

        // Antena + ponta luminosa
        shapeRenderer.setColor(0.75f, 0.80f, 0.88f, 1f);
        shapeRenderer.rectLine(x, cy + 16f * scale, x, cy + 28f * scale, 2.5f * scale);

        boolean blink = ((int) (animTime * 4f)) % 2 == 0;
        shapeRenderer.setColor(blink ? accentColor : Color.DARK_GRAY);
        shapeRenderer.circle(x, cy + 30f * scale, 4.5f * scale);

        // Bracinhos laterais
        shapeRenderer.setColor(0.22f, 0.72f, 0.92f, 1f);
        shapeRenderer.circle(x - 19f * scale, cy, 5.5f * scale);
        shapeRenderer.circle(x + 19f * scale, cy, 5.5f * scale);

        // Corpo/cabeca arredondado
        shapeRenderer.setColor(0.26f, 0.80f, 0.98f, 1f);
        shapeRenderer.circle(x, cy, 17f * scale);

        // Bochechas suaves
        shapeRenderer.setColor(1f, 0.55f, 0.65f, 0.65f);
        shapeRenderer.circle(x - 9f * scale, cy - 2f * scale, 3.2f * scale);
        shapeRenderer.circle(x + 9f * scale, cy - 2f * scale, 3.2f * scale);

        // Visor escuro
        shapeRenderer.setColor(0.08f, 0.12f, 0.18f, 1f);
        shapeRenderer.rect(x - 11f * scale, cy + 1f * scale, 22f * scale, 8f * scale);

        // Olhinhos brilhantes
        shapeRenderer.setColor(accentColor);
        shapeRenderer.circle(x - 5f * scale, cy + 5f * scale, 2.5f * scale);
        shapeRenderer.circle(x + 5f * scale, cy + 5f * scale, 2.5f * scale);
    }

    private void applyPendingRobotAction() {
        if (pendingAction == null) {
            return;
        }

        int r = pendingAction.getRow();
        int c = pendingAction.getCol();
        Cell target = board.getCell(r, c);
        float cx = cellCenterX(c);
        float cy = cellCenterY(r);

        if (pendingAction.getType() == RobotAction.ActionType.FLAG) {
            if (!target.isOpen()) {
                target.setFlagged(true);
                spawnBreakParticles(cx, cy, Color.GOLD);
            }
        } else if (pendingAction.getType() == RobotAction.ActionType.REVEAL) {
            board.openCell(r, c);
            spawnBreakParticles(cx, cy, target.isHasMine() ? Color.FIREBRICK : Color.SKY);
        }

        lastRobotMessage = "Historico (" + history.size() + "): " + pendingAction.toString();
        pendingAction = null;
        checkGameState("RC-8");
    }

    private void updateAnimations(float delta) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life -= delta;
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.vy -= 180f * delta;
            if (p.life <= 0f) {
                it.remove();
            }
        }

        updateExplosion(delta);

        if (robotPhase == RobotAnimPhase.MOVING && pendingAction != null) {
            float targetX = cellCenterX(pendingAction.getCol());
            float targetY = cellCenterY(pendingAction.getRow());
            float speed = 6.5f;

            robotX = MathUtils.lerp(robotX, targetX, Math.min(1f, delta * speed));
            robotY = MathUtils.lerp(robotY, targetY, Math.min(1f, delta * speed));

            float dist = (float) Math.hypot(targetX - robotX, targetY - robotY);
            if (dist < 3f) {
                robotX = targetX;
                robotY = targetY;
                robotPhase = RobotAnimPhase.ACTING;
                phaseTimer = 0.24f;
            }
        } else if (robotPhase == RobotAnimPhase.ACTING) {
            phaseTimer -= delta;
            if (phaseTimer <= 0f) {
                applyPendingRobotAction();
                robotPhase = RobotAnimPhase.IDLE;
            }
        }
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            resetBoard();
            return;
        }

        if (gameState != GameState.PLAYING) {
            return;
        }

        elapsedTime += delta;

        if (robotPhase == RobotAnimPhase.IDLE && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            RobotAction move = rc8Brain.planNextMove(board, history);
            if (move == null) {
                lastRobotMessage = "RC-8: sem deducao direta disponivel no momento.";
            } else {
                startRobotMove(move);
            }
        }

        if (Gdx.input.justTouched()) {
            float h = Gdx.graphics.getHeight();
            float currentBoardOffsetY = getCurrentBoardOffsetY();

            float screenX = Gdx.input.getX();
            float worldY = h - Gdx.input.getY();

            int col = (int) ((screenX - boardOffsetX) / (cellSize + margin));
            int row = (int) ((worldY - currentBoardOffsetY) / (cellSize + margin));

            if (row >= 0 && row < board.getRows() && col >= 0 && col < board.getCols()) {
                Cell cell = board.getCell(row, col);
                float cx = cellCenterX(col);
                float cy = cellCenterY(row);

                if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                    if (!cell.isOpen()) {
                        cell.setFlagged(!cell.isFlagged());
                        spawnBreakParticles(cx, cy, Color.GOLD);
                    }
                } else if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                    if (!cell.isOpen() && !cell.isFlagged()) {
                        board.openCell(row, col);
                        spawnBreakParticles(cx, cy, cell.isHasMine() ? Color.FIREBRICK : Color.LIGHT_GRAY);
                        checkGameState("Jogador");
                    }
                }
            }
        }
    }

    private void checkGameState(String actorName) {
        int rows = board.getRows();
        int cols = board.getCols();

        boolean hitMine = false;
        int hitRow = -1;
        int hitCol = -1;
        int closedSafeCells = 0;
        int openedSafeCells = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = board.getCell(r, c);
                if (cell.isHasMine()) {
                    if (cell.isOpen()) {
                        hitMine = true;
                        hitRow = r;
                        hitCol = c;
                    }
                } else {
                    if (cell.isOpen()) {
                        openedSafeCells++;
                    } else {
                        closedSafeCells++;
                    }
                }
            }
        }

        if (hitMine) {
            startExplosion(hitRow, hitCol);
            revealAllMines();
            gameState = GameState.GAME_OVER;
            robotPhase = RobotAnimPhase.IDLE;
            pendingAction = null;
            lastScore = openedSafeCells * 10;
            statusBanner = "GAME OVER! Mina detonada por " + actorName + " | Pts: " + lastScore + " | [N] para reiniciar";
            recordResult(actorName, lastScore);
            return;
        }

        if (closedSafeCells == 0) {
            gameState = GameState.VICTORY;
            robotPhase = RobotAnimPhase.IDLE;
            pendingAction = null;
            int timeBonus = Math.max(0, 300 - (int) elapsedTime * 5);
            lastScore = (openedSafeCells * 15) + timeBonus;
            statusBanner = "VITORIA! Setor limpo por " + actorName + " | Pts: " + lastScore + " | [N] nova partida";
            recordResult(actorName, lastScore);
        }
    }

    private void revealAllMines() {
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Cell cell = board.getCell(r, c);
                if (cell.isHasMine()) {
                    cell.setOpen(true);
                }
            }
        }
    }

    private void recordResult(String actorName, int score) {
        if (game == null || game.getSaveManager() == null || game.getSaveData() == null) {
            return;
        }

        SaveManager.SaveData data = game.getSaveData();
        data.scores.add(new ScoreEntry(actorName, level, score, elapsedTime));
        List<ScoreEntry> sorted = ScoreSorter.sortByEfficiency(data.scores);
        data.scores.clear();
        data.scores.addAll(sorted);

        if (gameState == GameState.VICTORY && data.unlockedLevel <= level) {
            data.unlockedLevel = level + 1;
        }

        game.getSaveManager().saveGame(data);
    }

    @Override
    public void render(float delta) {
        animTime += delta;
        handleInput(delta);
        updateAnimations(delta);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float currentBoardOffsetY = getCurrentBoardOffsetY();

        // Tremor de tela durante a explosao
        float shake = explosionActive ? 22f * Math.max(0f, 1f - explosionTime / 1.2f) : 0f;
        float sx = shake > 0f ? MathUtils.random(-shake, shake) : 0f;
        float sy = shake > 0f ? MathUtils.random(-shake, shake) : 0f;
        viewMatrix.setToOrtho2D(sx, sy, w, h);
        shapeRenderer.setProjectionMatrix(viewMatrix);
        batch.setProjectionMatrix(viewMatrix);

        Gdx.gl.glClearColor(0.09f, 0.11f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float gridW = board.getCols() * (cellSize + margin) + margin;
        float gridH = board.getRows() * (cellSize + margin) + margin;
        shapeRenderer.setColor(0.16f, 0.19f, 0.25f, 1f);
        shapeRenderer.rect(boardOffsetX - 6f, currentBoardOffsetY - 6f, gridW + 12f, gridH + 12f);

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Cell cell = board.getCell(r, c);
                float x = boardOffsetX + c * (cellSize + margin);
                float y = currentBoardOffsetY + r * (cellSize + margin);

                if (cell.isOpen()) {
                    shapeRenderer.setColor(cell.isHasMine() ? new Color(0.78f, 0.18f, 0.22f, 1f) : new Color(0.76f, 0.80f, 0.85f, 1f));
                } else if (cell.isFlagged()) {
                    shapeRenderer.setColor(new Color(0.95f, 0.76f, 0.18f, 1f));
                } else {
                    shapeRenderer.setColor(new Color(0.26f, 0.31f, 0.39f, 1f));
                }

                shapeRenderer.rect(x, y, cellSize, cellSize);
            }
        }

        if (pendingAction != null) {
            float tx = boardOffsetX + pendingAction.getCol() * (cellSize + margin);
            float ty = currentBoardOffsetY + pendingAction.getRow() * (cellSize + margin);
            shapeRenderer.setColor(0.25f, 0.85f, 1f, 0.28f);
            shapeRenderer.rect(tx - 2f, ty - 2f, cellSize + 4f, cellSize + 4f);
        }

        // Explosao: bola de fogo + fumaca + aneis (por cima do tabuleiro)
        drawFireball(w, h);
        drawRings();

        for (Particle p : particles) {
            float alpha = MathUtils.clamp(p.life / p.maxLife, 0f, 1f);
            shapeRenderer.setColor(p.color.r, p.color.g, p.color.b, alpha);
            shapeRenderer.rect(p.x - p.size * 0.5f, p.y - p.size * 0.5f, p.size, p.size);
        }

        // Robo RC-8
        float shakeX = (robotPhase == RobotAnimPhase.ACTING) ? MathUtils.random(-4f, 4f) : 0f;
        float shakeY = (robotPhase == RobotAnimPhase.ACTING) ? MathUtils.random(-4f, 4f) : 0f;
        float rx = robotX + shakeX;
        float ry = robotY + shakeY;

        Color accent;
        if (gameState == GameState.GAME_OVER) {
            accent = Color.RED;
        } else if (gameState == GameState.VICTORY) {
            accent = Color.LIME;
        } else if (pendingAction != null && pendingAction.getType() == RobotAction.ActionType.FLAG) {
            accent = Color.GOLD;
        } else {
            accent = Color.CYAN;
        }
        drawCuteRc8(rx, ry, 0.9f, accent);

        // Clarao branco do instante da explosao
        drawFlash(w, h);

        if (gameState != GameState.PLAYING) {
            shapeRenderer.setColor(0f, 0f, 0f, 0.72f);
            shapeRenderer.rect(35f, h - 105f, Gdx.graphics.getWidth() - 70f, 34f);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(
            batch,
            "RC-8: Protocolo Minado - Fase " + level + " | Tempo: " + String.format("%.1fs", elapsedTime),
            40f,
            h - 20f
        );
        font.draw(batch, lastRobotMessage, 40f, h - 46f);

        if (!statusBanner.isEmpty()) {
            font.setColor(gameState == GameState.VICTORY ? Color.LIME : Color.SALMON);
            font.draw(batch, statusBanner, 42f, h - 74f);
        }

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Cell cell = board.getCell(r, c);
                float x = boardOffsetX + c * (cellSize + margin);
                float y = currentBoardOffsetY + r * (cellSize + margin);

                if (cell.isOpen() && !cell.isHasMine() && cell.getNeighborMines() > 0) {
                    font.setColor(getNumberColor(cell.getNeighborMines()));
                    font.draw(batch, String.valueOf(cell.getNeighborMines()), x + 18f, y + 29f);
                } else if (cell.isOpen() && cell.isHasMine()) {
                    font.setColor(Color.WHITE);
                    font.draw(batch, "X", x + 17f, y + 29f);
                } else if (!cell.isOpen() && cell.isFlagged()) {
                    font.setColor(Color.BLACK);
                    font.draw(batch, "F", x + 18f, y + 29f);
                }
            }
        }
        batch.end();
    }

    private Color getNumberColor(int n) {
        switch (n) {
            case 1: return new Color(0.08f, 0.30f, 0.78f, 1f);
            case 2: return new Color(0.10f, 0.55f, 0.22f, 1f);
            case 3: return new Color(0.78f, 0.15f, 0.15f, 1f);
            case 4: return new Color(0.35f, 0.12f, 0.65f, 1f);
            default: return Color.DARK_GRAY;
        }
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
    }
}
