package com.benevides.rc8.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.benevides.rc8.Rc8Game;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Cutscene de abertura: lore da Fabrica Estrela do Sul + tutorial animado.
 * Estilo pixel art (sprites feitos de blocos), fogos, tremor de tela e
 * transicao em diamante. Nenhum asset externo e necessario.
 */
public class CutsceneScreen extends ScreenAdapter {

    private static final int TOTAL_SLIDES = 8;
    private static final float GROUND_Y = 180f;
    private static final float AREA_BOTTOM = 178f;

    private static final String[] TITLES = {
        "01 // FABRICA ESTRELA DO SUL",
        "02 // ALERTA VERMELHO",
        "03 // ENTRA RC-8",
        "TUTORIAL 1/4 // NUMEROS",
        "TUTORIAL 2/4 // BANDEIRAS",
        "TUTORIAL 3/4 // CASCATA DE ZEROS",
        "TUTORIAL 4/4 // CONTROLES E PERIGO",
        "MISSAO // PROTOCOLO MINADO"
    };
    private static final String[] SPEAKERS = {
        "NARRADOR", "SISTEMA", "RC-8", "RC-8", "RC-8", "RC-8", "RC-8", "NARRADOR"
    };
    private static final String[] TEXTS = {
        "Ha 40 anos, a Fabrica de Fogos Estrela do Sul pinta o ceu da cidade em cada festa. Esta noite, o espetaculo de teste era para ser perfeito.",
        "ALERTA! Uma falha no sistema de armazenagem armou dezenas de cargas instaveis pelo galpao. Nenhum humano pode entrar. Uma unica faisca e o bairro inteiro vira espetaculo.",
        "Eu sou o RC-8, robo desarmador. Meus sensores leem as 8 casas ao redor de cada bloco. Pequeno, antifaisca... e muito corajoso. Eu cuido disso.",
        "Ao abrir um bloco, o NUMERO diz quantas cargas existem nas 8 casas vizinhas. Aqui, o 2 avisa: duas cargas escondidas bem ao lado.",
        "Se um numero so tem uma vizinha fechada, ela e carga! Marque com BANDEIRA (clique direito) ou deixe o RC-8 fazer isso por voce.",
        "Bloco com ZERO nao tem carga por perto: ele abre os vizinhos em cascata, revelando uma area segura de uma vez so.",
        "Abrir uma carga e BOOM! Libere todos os blocos seguros para limpar o setor. Quando travar, peca ajuda ao RC-8.",
        "Setor por setor, vamos desarmar a fabrica. Respire fundo, RC-8... missao iniciada!"
    };
    private static final Color[] ACCENTS = {
        Color.GOLD, Color.RED, Color.CYAN, Color.CYAN, Color.GOLD, Color.LIME, Color.ORANGE, Color.GOLD
    };
    private static final Color[] FIREWORK_COLORS = {
        Color.GOLD, Color.ORANGE, Color.CYAN, Color.LIME, Color.PINK, Color.SKY, Color.SALMON
    };
    private static final Color[] ALARM_COLORS = {Color.RED, Color.ORANGE, Color.SCARLET};
    private static final Color[] NUM_COLORS = {
        new Color(0.08f, 0.30f, 0.78f, 1f), new Color(0.10f, 0.55f, 0.22f, 1f),
        new Color(0.78f, 0.15f, 0.15f, 1f), new Color(0.35f, 0.12f, 0.65f, 1f)
    };

    // ---------------------------------------------------------------- sprites
    private static final String[] RC8_BASE = {
        "......aa......",
        "......gg......",
        "...oooooooo...",
        "..obbbbbbbbo..",
        ".oblllbbbbbbo.",
        ".obvvvvvvvvbo.",
        ".obvevvvvevbo.",
        ".obvevvvvevbo.",
        "cobvvvvvvvvboc",
        "cobpbbbbbbpboc",
        ".obbbbbbbbbbo.",
        "..obbbbbbbbo..",
        "...oddddddo...",
        "PLACEHOLDER..."
    };
    private static final String[] RC8_IDLE = rc8("...oo....oo...");
    private static final String[] RC8_STEP_A = rc8("..oo......oo..");
    private static final String[] RC8_STEP_B = rc8("....oo..oo....");

    private static final String[] BOMB = {
        "........s...",
        ".......ff...",
        "...oooooo...",
        "..okkkkkko..",
        ".okwwkkkkko.",
        ".okwkkkkkko.",
        ".okkkrrkkko.",
        ".okkkrrkkko.",
        ".okkkkkkkko.",
        "..okkkkkko..",
        "...oooooo..."
    };
    private static final String[] FLAG = {
        "..gyyyyyy.",
        "..gyyyyyh.",
        "..gyyyyh..",
        "..gyyyh...",
        "..gyyh....",
        "..gyh.....",
        "..gh......",
        "..g.......",
        "..g.......",
        ".ggg......",
        "gggggg...."
    };
    private static final String[][] DIGITS = {
        {".#.", "##.", ".#.", ".#.", "###"},
        {"###", "..#", "###", "#..", "###"},
        {"###", "..#", "###", "..#", "###"},
        {"#.#", "#.#", "###", "..#", "..#"}
    };

    private static String[] rc8(String feet) {
        String[] s = RC8_BASE.clone();
        s[s.length - 1] = feet;
        return s;
    }

    // ---------------------------------------------------------------- tipos
    private static class Spark {
        float x, y, vx, vy, life, maxLife, size, gravity;
        Color color;
    }

    private static class Rocket {
        float x, y, vy, targetY, trailT;
        Color color;
    }

    private static class TextItem {
        BitmapFont f;
        String s;
        float x, y, r, g, b, a;
    }

    // ---------------------------------------------------------------- estado
    private final Rc8Game game;
    private final int level;
    private final int robotLevel;

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont titleFont;
    private BitmapFont bigFont;
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 viewMatrix = new Matrix4();

    private int currentScene = 0;
    private int pendingScene = 0;
    private int transState = -1; // 1 fechando, -1 abrindo, 0 nada
    private float transT = 1f;

    private float animTime = 0f;
    private float sceneTime = 0f;
    private float prevSceneTime = 0f;
    private float charsShown = 0f;
    private int ps = 5;
    private float sw = 800f;
    private float sh = 600f;
    private float shakeKick = 0f;
    private float launchTimer = 0.4f;

    private final List<Spark> sparks = new ArrayList<>();
    private final List<Rocket> rockets = new ArrayList<>();
    private final List<TextItem> textQueue = new ArrayList<>();

    private final float[] starX = new float[70];
    private final float[] starY = new float[70];
    private final float[] starPh = new float[70];

    // sprite palette state
    private Color spEye = Color.CYAN;
    private Color spAnt = Color.CYAN;
    private float spDim = 1f;
    private float spAlpha = 1f;

    // mini tabuleiros das demos
    private float demoX, demoY, demoCs, demoPitch;
    private boolean[][] demo1, demo2, demo3;
    private int[][] cascadeDist;
    private int cascadeMax;

    public CutsceneScreen(Rc8Game game, int level, int robotLevel) {
        this.game = game;
        this.level = level;
        this.robotLevel = robotLevel;
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        font = new BitmapFont();
        font.getData().setScale(1.25f);
        titleFont = new BitmapFont();
        titleFont.getData().setScale(1.7f);
        bigFont = new BitmapFont();
        bigFont.getData().setScale(3f);

        for (int i = 0; i < starX.length; i++) {
            starX[i] = MathUtils.random();
            starY[i] = MathUtils.random();
            starPh[i] = MathUtils.random(0f, 6.28f);
        }
        buildDemos();
        enterScene();
        transState = -1;
        transT = 1f;
    }

    private void goToGame() {
        game.setScreen(new GameScreen(game, level, robotLevel));
        dispose();
    }

    private void requestScene(int target) {
        if (target < 0 || transState != 0) {
            return;
        }
        if (target >= TOTAL_SLIDES) {
            goToGame();
            return;
        }
        pendingScene = target;
        transState = 1;
        transT = 0f;
    }

    private void enterScene() {
        sceneTime = 0f;
        prevSceneTime = 0f;
        charsShown = 0f;
        sparks.clear();
        rockets.clear();
        launchTimer = 0.4f;
        shakeKick = 0f;
    }

    // ---------------------------------------------------------------- render
    @Override
    public void render(float delta) {
        if (delta > 0.05f) {
            delta = 0.05f;
        }
        animTime += delta;
        prevSceneTime = sceneTime;
        sceneTime += delta;
        sw = Gdx.graphics.getWidth();
        sh = Gdx.graphics.getHeight();
        float w = sw;
        float h = sh;
        ps = Math.max(3, Math.round(Math.min(w, h) / 120f));

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            goToGame();
            return;
        }

        if (transState == 0) {
            boolean advance = Gdx.input.justTouched()
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT);
            boolean back = Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE);
            if (advance) {
                if (charsShown < TEXTS[currentScene].length()) {
                    charsShown = TEXTS[currentScene].length();
                } else {
                    requestScene(currentScene + 1);
                    if (currentScene + 1 >= TOTAL_SLIDES) {
                        return;
                    }
                }
            } else if (back) {
                requestScene(currentScene - 1);
            }
            charsShown += delta * 45f;
        } else if (transState == 1) {
            transT += delta / 0.35f;
            if (transT >= 1f) {
                currentScene = pendingScene;
                enterScene();
                transState = -1;
                transT = 1f;
            }
        } else {
            transT -= delta / 0.35f;
            if (transT <= 0f) {
                transT = 0f;
                transState = 0;
            }
        }

        updateFireworks(delta);
        shakeKick = Math.max(0f, shakeKick - delta * 30f);
        float shk = Math.max(shakeKick, currentScene == 1 ? 2f : 0f);
        float ox = shk > 0f ? MathUtils.round(MathUtils.random(-shk, shk)) : 0f;
        float oy = shk > 0f ? MathUtils.round(MathUtils.random(-shk, shk)) : 0f;
        viewMatrix.setToOrtho2D(ox, oy, w, h);
        shapeRenderer.setProjectionMatrix(viewMatrix);
        batch.setProjectionMatrix(viewMatrix);

        textQueue.clear();
        Gdx.gl.glClearColor(0.03f, 0.04f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        switch (currentScene) {
            case 0: sceneFactoryIntro(w, h); break;
            case 1: sceneAlarm(w, h); break;
            case 2: sceneBoot(w, h); break;
            case 3: sceneNumbers(w, h); break;
            case 4: sceneFlags(w, h); break;
            case 5: sceneCascade(w, h); break;
            case 6: sceneControls(w, h); break;
            default: sceneFinale(w, h); break;
        }
        drawSparks();
        drawUi(w, h);
        shapeRenderer.end();

        batch.begin();
        for (TextItem t : textQueue) {
            t.f.setColor(t.r, t.g, t.b, t.a);
            t.f.draw(batch, t.s, t.x, t.y);
        }
        int n = Math.min(TEXTS[currentScene].length(), (int) charsShown);
        font.setColor(Color.WHITE);
        font.draw(batch, TEXTS[currentScene], 40f, 18f + 130f - 18f, 0, n, w - 80f, Align.left, true);
        batch.end();

        if (transState != 0) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            drawTransition(w, h);
            shapeRenderer.end();
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ---------------------------------------------------------------- helpers
    private void fill(float x, float y, float w, float h) {
        int x0 = MathUtils.round(x);
        int y0 = MathUtils.round(y);
        int x1 = MathUtils.round(x + w);
        int y1 = MathUtils.round(y + h);
        shapeRenderer.rect(x0, y0, x1 - x0, y1 - y0);
    }

    private void col(float r, float g, float b, float a) {
        shapeRenderer.setColor(r, g, b, a);
    }

    private float smooth(float a, float b, float t) {
        float k = MathUtils.clamp((t - a) / (b - a), 0f, 1f);
        return k * k * (3f - 2f * k);
    }

    private boolean crossed(float loop, float thr) {
        float a = prevSceneTime % loop;
        float b = sceneTime % loop;
        return a < thr && b >= thr;
    }

    private float areaMidY(float h) {
        return (AREA_BOTTOM + (h - 60f)) * 0.5f;
    }

    private void queueText(BitmapFont f, String s, float x, float y, float r, float g, float b, float a) {
        TextItem t = new TextItem();
        t.f = f;
        t.s = s;
        t.x = x;
        t.y = y;
        t.r = r;
        t.g = g;
        t.b = b;
        t.a = a;
        textQueue.add(t);
    }

    private void queueText(BitmapFont f, String s, float x, float y, Color c) {
        queueText(f, s, x, y, c.r, c.g, c.b, c.a);
    }

    private void queueTextCentered(BitmapFont f, String s, float cx, float y, Color c) {
        layout.setText(f, s);
        queueText(f, s, cx - layout.width * 0.5f, y, c);
    }

    private void drawFrame(float x, float y, float w, float h, float t, Color c, float a) {
        shapeRenderer.setColor(c.r, c.g, c.b, a);
        fill(x, y, w, t);
        fill(x, y + h - t, w, t);
        fill(x, y, t, h);
        fill(x + w - t, y, t, h);
    }

    private void drawDigit(int n, float cx, float cy, float px, Color c) {
        if (n < 1 || n > 4) {
            return;
        }
        String[] g = DIGITS[n - 1];
        shapeRenderer.setColor(c);
        float x0 = cx - 1.5f * px;
        float y0 = cy - 2.5f * px;
        for (int r = 0; r < 5; r++) {
            for (int k = 0; k < 3; k++) {
                if (g[r].charAt(k) == '#') {
                    fill(x0 + k * px, y0 + (4 - r) * px, px, px);
                }
            }
        }
    }

    private void pixelCircle(float cx, float cy, float rad, float step) {
        for (float dy = -rad; dy < rad; dy += step) {
            float half = (float) Math.sqrt(Math.max(0f, rad * rad - dy * dy));
            fill(cx - half, cy + dy, half * 2f, step);
        }
    }

    // ---------------------------------------------------------------- sprites
    private void sc(float r, float g, float b) {
        shapeRenderer.setColor(r * spDim, g * spDim, b * spDim, spAlpha);
    }

    private boolean setSpriteColor(char c) {
        switch (c) {
            case 'o': sc(0.07f, 0.09f, 0.17f); return true;
            case 'b': sc(0.26f, 0.80f, 0.98f); return true;
            case 'l': sc(0.70f, 0.95f, 1.00f); return true;
            case 'd': sc(0.14f, 0.52f, 0.78f); return true;
            case 'c': sc(0.20f, 0.66f, 0.90f); return true;
            case 'v': sc(0.06f, 0.08f, 0.14f); return true;
            case 'p': sc(1.00f, 0.55f, 0.65f); return true;
            case 'g': sc(0.62f, 0.68f, 0.78f); return true;
            case 'k': sc(0.20f, 0.22f, 0.30f); return true;
            case 'w': sc(0.55f, 0.60f, 0.72f); return true;
            case 'r': sc(0.95f, 0.20f, 0.20f); return true;
            case 'f': sc(0.70f, 0.52f, 0.30f); return true;
            case 'y': sc(0.98f, 0.80f, 0.20f); return true;
            case 'h': sc(0.90f, 0.50f, 0.10f); return true;
            case 'e': sc(spEye.r, spEye.g, spEye.b); return true;
            case 'a':
            case 's': sc(spAnt.r, spAnt.g, spAnt.b); return true;
            default: return false;
        }
    }

    private void drawSprite(String[] s, float cx, float bottom, int px) {
        int rows = s.length;
        int cols = s[0].length();
        float x0 = cx - cols * px * 0.5f;
        for (int i = 0; i < rows; i++) {
            String row = s[i];
            float y = bottom + (rows - 1 - i) * px;
            for (int j = 0; j < row.length(); j++) {
                char c = row.charAt(j);
                if (c != '.' && setSpriteColor(c)) {
                    fill(x0 + j * px, y, px, px);
                }
            }
        }
    }

    /** frame: 0 parado, 1 e 2 caminhando. */
    private void drawRobot(float cx, float bottom, int px, Color acc, int frame) {
        boolean antOn = ((int) (animTime * 4f)) % 2 == 0;
        boolean blink = (animTime % 3.2f) < 0.12f;
        spAnt = antOn ? acc : Color.DARK_GRAY;
        spEye = blink ? Color.BLACK : acc;
        String[] spr = frame == 1 ? RC8_STEP_A : (frame == 2 ? RC8_STEP_B : RC8_IDLE);
        int bob = (frame == 0 && Math.sin(animTime * 5f) > 0) ? px : 0;
        // sombra
        shapeRenderer.setColor(0f, 0f, 0f, 0.35f);
        fill(cx - 5 * px, bottom - px, 10 * px, px);
        drawSprite(spr, cx, bottom + bob, px);
    }

    private void drawBomb(float cx, float bottom, int px, float alpha) {
        spAlpha = alpha;
        spAnt = ((int) (animTime * 6f)) % 2 == 0 ? Color.ORANGE : Color.YELLOW;
        drawSprite(BOMB, cx, bottom, px);
        spAlpha = 1f;
    }

    private void drawWarning(float cx, float cy, float s) {
        shapeRenderer.setColor(0.07f, 0.05f, 0.05f, 1f);
        shapeRenderer.triangle(cx, cy + s * 1.15f, cx - s * 1.1f, cy - s * 0.8f, cx + s * 1.1f, cy - s * 0.8f);
        shapeRenderer.setColor(1f, 0.82f, 0.15f, 1f);
        shapeRenderer.triangle(cx, cy + s * 0.95f, cx - s * 0.9f, cy - s * 0.62f, cx + s * 0.9f, cy - s * 0.62f);
        shapeRenderer.setColor(0.07f, 0.05f, 0.05f, 1f);
        fill(cx - s * 0.1f, cy - s * 0.05f, s * 0.2f, s * 0.6f);
        fill(cx - s * 0.1f, cy - s * 0.45f, s * 0.2f, s * 0.2f);
    }

    // ---------------------------------------------------------------- tiles
    private void drawTile(float x, float y, float s, int type) {
        float bv = Math.max(2, Math.round(ps * 0.6f));
        float[] base, hi, lo;
        switch (type) {
            case 1:
                base = new float[]{0.76f, 0.80f, 0.85f};
                lo = new float[]{0.88f, 0.91f, 0.95f};
                hi = new float[]{0.60f, 0.64f, 0.70f};
                break;
            case 2:
                base = new float[]{0.95f, 0.76f, 0.18f};
                hi = new float[]{1f, 0.90f, 0.45f};
                lo = new float[]{0.70f, 0.50f, 0.08f};
                break;
            case 3:
                base = new float[]{0.78f, 0.18f, 0.22f};
                hi = new float[]{0.95f, 0.40f, 0.40f};
                lo = new float[]{0.50f, 0.08f, 0.12f};
                break;
            default:
                base = new float[]{0.26f, 0.31f, 0.39f};
                hi = new float[]{0.40f, 0.47f, 0.58f};
                lo = new float[]{0.15f, 0.18f, 0.25f};
                break;
        }
        shapeRenderer.setColor(base[0], base[1], base[2], 1f);
        fill(x, y, s, s);
        shapeRenderer.setColor(hi[0], hi[1], hi[2], 1f);
        fill(x, y + s - bv, s, bv);
        fill(x, y, bv, s);
        shapeRenderer.setColor(lo[0], lo[1], lo[2], 1f);
        fill(x, y, s, bv);
        fill(x + s - bv, y, bv, s);
    }

    private void layoutDemo(int cols, int rows, float cs, float cx, float cy) {
        demoCs = cs;
        float gap = Math.max(2, ps / 2);
        demoPitch = cs + gap;
        demoX = cx - (cols * demoPitch - gap) * 0.5f;
        demoY = cy - (rows * demoPitch - gap) * 0.5f;
    }

    private float tx(int c) {
        return demoX + c * demoPitch;
    }

    private float ty(int r, int rows) {
        return demoY + (rows - 1 - r) * demoPitch;
    }

    private float tcx(int c) {
        return tx(c) + demoCs * 0.5f;
    }

    private float tcy(int r, int rows) {
        return ty(r, rows) + demoCs * 0.5f;
    }

    private void bombInTile(int r, int c, int rows, float alpha) {
        int px = Math.max(2, (int) (demoCs / 14f));
        drawBomb(tcx(c), tcy(r, rows) - BOMB.length * px * 0.5f, px, alpha);
    }

    private int robotPx() {
        return Math.max(2, (int) (demoCs / 14f));
    }

    // ---------------------------------------------------------------- demos
    private boolean[][] makeMines(int rows, int cols, int[][] pos) {
        boolean[][] m = new boolean[rows][cols];
        for (int[] p : pos) {
            m[p[0]][p[1]] = true;
        }
        return m;
    }

    private int countAround(boolean[][] m, int r, int c) {
        int n = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = r + dr;
                int nc = c + dc;
                if ((dr != 0 || dc != 0) && nr >= 0 && nc >= 0 && nr < m.length && nc < m[0].length && m[nr][nc]) {
                    n++;
                }
            }
        }
        return n;
    }

    private void buildDemos() {
        demo1 = makeMines(3, 5, new int[][]{{0, 3}, {2, 3}});
        demo2 = makeMines(3, 5, new int[][]{{1, 3}});
        demo3 = makeMines(4, 8, new int[][]{{0, 6}, {2, 5}, {3, 2}});

        int rows = 4;
        int cols = 8;
        cascadeDist = new int[rows][cols];
        for (int[] row : cascadeDist) {
            java.util.Arrays.fill(row, -1);
        }
        ArrayDeque<int[]> q = new ArrayDeque<>();
        cascadeDist[0][0] = 0;
        q.add(new int[]{0, 0});
        while (!q.isEmpty()) {
            int[] p = q.poll();
            cascadeMax = Math.max(cascadeMax, cascadeDist[p[0]][p[1]]);
            if (countAround(demo3, p[0], p[1]) != 0) {
                continue;
            }
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = p[0] + dr;
                    int nc = p[1] + dc;
                    if (nr >= 0 && nc >= 0 && nr < rows && nc < cols
                        && cascadeDist[nr][nc] == -1 && !demo3[nr][nc]) {
                        cascadeDist[nr][nc] = cascadeDist[p[0]][p[1]] + 1;
                        q.add(new int[]{nr, nc});
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- fogos
    private boolean fireworksActive() {
        return currentScene == 0 || currentScene == 1 || currentScene == 7;
    }

    private void updateFireworks(float delta) {
        if (fireworksActive() && transState != 1) {
            launchTimer -= delta;
            if (launchTimer <= 0f) {
                launchTimer = currentScene == 1 ? MathUtils.random(0.35f, 0.8f) : MathUtils.random(0.6f, 1.3f);
                Rocket r = new Rocket();
                r.x = MathUtils.random(sw * 0.12f, sw * 0.88f);
                r.y = GROUND_Y + 60f;
                r.vy = MathUtils.random(330f, 430f);
                r.targetY = MathUtils.random(sh * 0.52f, sh * 0.78f);
                Color[] pal = currentScene == 1 ? ALARM_COLORS : FIREWORK_COLORS;
                r.color = pal[MathUtils.random(pal.length - 1)];
                rockets.add(r);
            }
        }

        Iterator<Rocket> it = rockets.iterator();
        while (it.hasNext()) {
            Rocket r = it.next();
            r.y += r.vy * delta;
            r.trailT -= delta;
            if (r.trailT <= 0f) {
                r.trailT = 0.03f;
                addSpark(r.x, r.y, MathUtils.random(-12f, 12f), -30f, 0.4f, ps * 0.8f, Color.ORANGE, 40f);
            }
            if (r.y >= r.targetY) {
                int n = 34;
                float spd = ps * MathUtils.random(24f, 34f);
                for (int i = 0; i < n; i++) {
                    float a = i * MathUtils.PI2 / n + MathUtils.random(-0.08f, 0.08f);
                    float s = spd * MathUtils.random(0.65f, 1f);
                    addSpark(r.x, r.y, MathUtils.cos(a) * s, MathUtils.sin(a) * s,
                        MathUtils.random(1f, 1.6f), ps * 0.9f, r.color, 70f);
                }
                if (currentScene == 1) {
                    shakeKick = 9f;
                }
                it.remove();
            }
        }

        Iterator<Spark> sit = sparks.iterator();
        while (sit.hasNext()) {
            Spark s = sit.next();
            s.life -= delta;
            s.x += s.vx * delta;
            s.y += s.vy * delta;
            s.vy -= s.gravity * delta;
            s.vx *= 1f - 0.8f * delta;
            if (s.life <= 0f) {
                sit.remove();
            }
        }
    }

    private void addSpark(float x, float y, float vx, float vy, float life, float size, Color c, float g) {
        Spark s = new Spark();
        s.x = x;
        s.y = y;
        s.vx = vx;
        s.vy = vy;
        s.life = life;
        s.maxLife = life;
        s.size = size;
        s.color = c;
        s.gravity = g;
        sparks.add(s);
    }

    /** Estouro pequeno para os pops das demos. */
    private void pop(float x, float y, Color c, int n) {
        for (int i = 0; i < n; i++) {
            float a = MathUtils.random(0f, MathUtils.PI2);
            float s = ps * MathUtils.random(8f, 20f);
            addSpark(x, y, MathUtils.cos(a) * s, MathUtils.sin(a) * s, MathUtils.random(0.35f, 0.7f), ps * 0.8f, c, 120f);
        }
    }

    private void drawSparks() {
        for (Spark s : sparks) {
            float a = MathUtils.clamp(s.life / s.maxLife, 0f, 1f);
            shapeRenderer.setColor(s.color.r, s.color.g, s.color.b, a);
            fill(s.x - s.size * 0.5f, s.y - s.size * 0.5f, s.size, s.size);
        }
    }

    // ---------------------------------------------------------------- cenario
    private void drawNightSky(float w, float h, float alarm) {
        int bands = 18;
        float bh = h / bands;
        for (int i = 0; i < bands; i++) {
            float t = i / (float) (bands - 1);
            float r = MathUtils.lerp(0.26f, 0.03f, t) + alarm * 0.30f * (1f - t * 0.5f);
            float g = MathUtils.lerp(0.14f, 0.04f, t) * (1f - alarm * 0.4f);
            float b = MathUtils.lerp(0.32f, 0.11f, t) * (1f - alarm * 0.4f);
            shapeRenderer.setColor(r, g, b, 1f);
            fill(-60, i * bh, w + 120, bh + 1f);
        }
        for (int i = 0; i < starX.length; i++) {
            float tw = 0.5f + 0.5f * MathUtils.sin(animTime * 2f + starPh[i]);
            shapeRenderer.setColor(1f, 1f, 0.9f, 0.25f + 0.75f * tw);
            float size = (i % 6 == 0) ? ps : Math.max(2, ps * 2 / 3);
            fill(starX[i] * w, h * (0.32f + 0.68f * starY[i] * 0.9f), size, size);
        }
        // lua
        float mx = w * 0.84f;
        float my = h * 0.76f;
        shapeRenderer.setColor(0.95f, 0.93f, 0.80f, 1f);
        pixelCircle(mx, my, ps * 6f, Math.max(2, ps));
        shapeRenderer.setColor(0.82f, 0.80f, 0.66f, 1f);
        fill(mx - ps * 2f, my + ps, ps * 2f, ps * 2f);
        fill(mx + ps, my - ps * 2f, ps * 2f, ps);
    }

    private void drawGroundAndHills(float w, float alarm) {
        shapeRenderer.setColor(0.10f + alarm * 0.06f, 0.08f, 0.16f, 1f);
        float step = ps * 3f;
        for (float x = 0; x < w; x += step) {
            float hh = 34f + 16f * MathUtils.sin(x * 0.012f) + 9f * MathUtils.sin(x * 0.031f + 1.3f);
            fill(x, GROUND_Y - 4f, step + 1f, hh + 4f);
        }
        shapeRenderer.setColor(0.04f, 0.05f, 0.08f, 1f);
        fill(-60, 0, w + 120, GROUND_Y);
        shapeRenderer.setColor(0.09f, 0.10f, 0.15f, 1f);
        fill(-60, GROUND_Y - ps, w + 120, ps);
    }

    private void drawFactory(float w, boolean alarm, float gate) {
        float u = ps * 4f;
        float hallW = Math.min(w * 0.64f, u * 30f);
        float hallH = u * 5f;
        float fx = (w - hallW) * 0.5f;
        float gy = GROUND_Y;
        float pulse = 0.5f + 0.5f * MathUtils.sin(animTime * 8f);

        float[] cf = {0.14f, 0.50f, 0.86f};
        float[] chH = {1.5f, 1.9f, 1.4f};
        for (int i = 0; i < 3; i++) {
            float cw = u * 0.9f;
            float cx = fx + hallW * cf[i] - cw * 0.5f;
            float top = gy + hallH * chH[i];
            shapeRenderer.setColor(0.14f, 0.15f, 0.22f, 1f);
            fill(cx, gy, cw, top - gy);
            for (int s = 0; s < 3; s++) {
                if (s % 2 == 0) {
                    shapeRenderer.setColor(0.80f, 0.20f, 0.20f, 1f);
                } else {
                    shapeRenderer.setColor(0.90f, 0.90f, 0.92f, 1f);
                }
                fill(cx, top - (s + 1) * ps * 2f, cw, ps * 2f);
            }
            for (int k = 0; k < 5; k++) {
                float t = (animTime * 0.35f + k / 5f + i * 0.27f) % 1f;
                float size = ps * (2f + t * 5f);
                float smx = cx + cw * 0.5f + MathUtils.sin(t * 5f + i) * ps * 3f * t - size * 0.5f;
                float smy = top + t * u * 5f;
                float a = 0.38f * (1f - t);
                if (alarm) {
                    shapeRenderer.setColor(0.35f, 0.10f, 0.10f, a);
                } else {
                    shapeRenderer.setColor(0.45f, 0.45f, 0.55f, a);
                }
                fill(smx, smy, size, size);
            }
        }

        shapeRenderer.setColor(0.11f, 0.12f, 0.19f, 1f);
        fill(fx, gy, hallW, hallH);
        shapeRenderer.setColor(0.08f, 0.09f, 0.14f, 1f);
        fill(fx, gy, hallW, ps * 2f);

        int teeth = 6;
        float tw = hallW / teeth;
        float th = u * 0.9f;
        shapeRenderer.setColor(0.13f, 0.14f, 0.22f, 1f);
        for (int t = 0; t < teeth; t++) {
            float x0 = fx + t * tw;
            shapeRenderer.triangle(x0, gy + hallH, x0 + tw, gy + hallH, x0 + tw, gy + hallH + th);
        }

        int cols = 10;
        float sp = hallW / (cols + 1);
        float ws = Math.min(ps * 2.4f, sp * 0.55f);
        for (int i = 0; i < cols; i++) {
            boolean lit = ((i * 7) % 5 != 0) && (((int) (animTime * 1.5f + i * 1.7f)) % 9 != 0);
            if (!lit) {
                shapeRenderer.setColor(0.18f, 0.19f, 0.26f, 1f);
            } else if (alarm) {
                shapeRenderer.setColor(0.95f, 0.15f + 0.1f * pulse, 0.15f, 1f);
            } else {
                shapeRenderer.setColor(1f, 0.84f, 0.40f, 1f);
            }
            fill(fx + sp * (i + 1) - ws * 0.5f, gy + hallH - ws * 2.8f, ws, ws * 1.3f);
        }

        float pw = hallW * 0.52f;
        float ph = u * 1.2f;
        float px0 = fx + (hallW - pw) * 0.5f;
        float py0 = gy + u * 2.5f;
        shapeRenderer.setColor(0.85f, 0.65f, 0.15f, 1f);
        fill(px0 - ps, py0 - ps, pw + ps * 2f, ph + ps * 2f);
        shapeRenderer.setColor(0.06f, 0.07f, 0.11f, 1f);
        fill(px0, py0, pw, ph);
        queueTextCentered(font, "FABRICA ESTRELA DO SUL", w * 0.5f, py0 + ph * 0.5f + 9f,
            alarm ? Color.SALMON : Color.GOLD);

        float gw = u * 2f;
        float gh = u * 2.2f;
        float gx = w * 0.5f - gw * 0.5f;
        shapeRenderer.setColor(0.03f, 0.04f, 0.07f, 1f);
        fill(gx, gy, gw, gh);
        if (gate > 0f) {
            shapeRenderer.setColor(1f, 0.88f, 0.45f, gate);
            fill(gx + ps, gy, gw - ps * 2f, gh - ps);
            shapeRenderer.setColor(1f, 0.9f, 0.5f, 0.12f * gate);
            shapeRenderer.triangle(w * 0.5f, gy + gh, w * 0.5f - u * 4f, gy + gh + u * 8f, w * 0.5f + u * 4f, gy + gh + u * 8f);
        }

        if (alarm) {
            float bx = w * 0.5f;
            float by = gy + hallH + th + ps * 3f;
            shapeRenderer.setColor(0.25f, 0.25f, 0.30f, 1f);
            fill(bx - ps * 2f, by - ps * 3f, ps * 4f, ps * 3f);
            shapeRenderer.setColor(1f, 0.2f, 0.2f, 0.6f + 0.4f * pulse);
            shapeRenderer.circle(bx, by + ps, ps * 2.5f);
            for (int k = 0; k < 2; k++) {
                float ang = animTime * 4f + k * MathUtils.PI;
                float len = w * 0.7f;
                shapeRenderer.setColor(1f, 0.15f, 0.15f, 0.16f);
                shapeRenderer.triangle(bx, by + ps,
                    bx + MathUtils.cos(ang - 0.18f) * len, by + ps + MathUtils.sin(ang - 0.18f) * len,
                    bx + MathUtils.cos(ang + 0.18f) * len, by + ps + MathUtils.sin(ang + 0.18f) * len);
            }
        }

        shapeRenderer.setColor(0.05f, 0.06f, 0.09f, 1f);
        float post = ps * 4f;
        for (float x = 0; x < w; x += post) {
            fill(x, gy - ps, ps, ps * 6f);
        }
        fill(0, gy + ps * 3f, w, ps);
    }

    // ---------------------------------------------------------------- cena 0
    private void sceneFactoryIntro(float w, float h) {
        drawNightSky(w, h, 0f);
        drawGroundAndHills(w, 0f);
        drawFactory(w, false, 0f);
    }

    // ---------------------------------------------------------------- cena 1
    private void sceneAlarm(float w, float h) {
        float al = 0.5f + 0.5f * MathUtils.sin(animTime * 6f);
        drawNightSky(w, h, 0.5f + 0.5f * al);
        drawGroundAndHills(w, 0.6f);
        drawFactory(w, true, 0f);
        shapeRenderer.setColor(1f, 0f, 0f, 0.05f + 0.08f * al);
        fill(-60, -60, w + 120, h + 120);
        float s = ps * 9f * (1f + 0.06f * MathUtils.sin(animTime * 10f));
        drawWarning(w * 0.16f, h * 0.60f, s);
        queueTextCentered(font, "ARMAZENAMENTO EM FALHA", w * 0.16f, h * 0.60f - s - 12f, Color.SALMON);
    }

    // ---------------------------------------------------------------- cena 2
    private void sceneBoot(float w, float h) {
        float tb = sceneTime;
        float cx = w * 0.5f;
        float floorY = AREA_BOTTOM + ps * 2f;
        shapeRenderer.setColor(0.04f, 0.05f, 0.09f, 1f);
        fill(-60, -60, w + 120, h + 120);

        float beam = 0.04f + 0.08f * smooth(0.3f, 1.3f, tb);
        shapeRenderer.setColor(1f, 0.97f, 0.8f, beam);
        shapeRenderer.triangle(cx, h - 60f, cx - ps * 20f, floorY, cx + ps * 20f, floorY);

        float dockW = ps * 22f;
        shapeRenderer.setColor(0.16f, 0.19f, 0.26f, 1f);
        fill(cx - dockW * 0.5f, floorY, dockW, ps * 3f);
        for (int i = 0; i < 11; i++) {
            if (i % 2 == 0) {
                shapeRenderer.setColor(0.95f, 0.76f, 0.18f, 1f);
            } else {
                shapeRenderer.setColor(0.10f, 0.10f, 0.12f, 1f);
            }
            fill(cx - dockW * 0.5f + i * ps * 2f, floorY + ps * 2f, ps * 2f, ps);
        }

        int px = Math.round(ps * 1.6f);
        spDim = tb < 0.6f ? 0.25f : 1f;
        boolean eyesOn = tb > 1.0f && !(tb > 1.15f && tb < 1.3f);
        boolean antOn = tb > 1.8f;
        spEye = eyesOn ? Color.CYAN : Color.BLACK;
        spAnt = antOn && ((int) (animTime * 4f)) % 2 == 0 ? Color.CYAN : Color.DARK_GRAY;
        int bob = (tb > 1.8f && Math.sin(animTime * 5f) > 0) ? px : 0;
        drawSprite(RC8_IDLE, cx, floorY + ps * 3f + bob, px);
        spDim = 1f;

        if (crossed(100f, 1.3f)) {
            pop(cx, floorY + ps * 14f, Color.CYAN, 22);
            shakeKick = 5f;
        }
        float rt = tb - 1.3f;
        if (rt > 0f && rt < 1f) {
            shapeRenderer.setColor(0.3f, 0.9f, 1f, 1f - rt);
            float rad = rt * ps * 30f;
            float cy = floorY + ps * 14f;
            for (int i = 0; i < 40; i++) {
                float a = i * MathUtils.PI2 / 40f;
                fill(cx + MathUtils.cos(a) * rad, cy + MathUtils.sin(a) * rad, ps, ps);
            }
        }

        String[] lines = {
            "> RC-8 v8.0 ONLINE",
            "> SENSOR 8 VIAS .... OK",
            "> MODO DESARMAR .... OK",
            "> ANTIFAISCA ....... OK",
            "> CORAGEM .......... 100%"
        };
        float lx = w * 0.05f;
        float ly = areaMidY(h) + 70f;
        for (int i = 0; i < lines.length; i++) {
            int n = (int) ((tb - (0.5f + i * 0.7f)) * 30f);
            if (n > 0) {
                String s = lines[i].substring(0, Math.min(n, lines[i].length()));
                queueText(font, s, lx, ly - i * 26f, Color.LIME);
            }
        }

        String[] names = {"SENSOR", "VELOCIDADE", "CORAGEM"};
        float[] vals = {0.9f, 0.6f, 1f};
        float bx = w * 0.72f;
        float bw = ps * 24f;
        for (int i = 0; i < 3; i++) {
            float by = ly - 10f - i * 50f;
            queueText(font, names[i], bx, by + 30f, Color.LIGHT_GRAY);
            shapeRenderer.setColor(0.16f, 0.19f, 0.26f, 1f);
            fill(bx, by, bw, ps * 2f);
            shapeRenderer.setColor(0.26f, 0.80f, 0.98f, 1f);
            fill(bx, by, bw * vals[i] * smooth(1.5f + i * 0.5f, 2.5f + i * 0.5f, tb), ps * 2f);
        }
    }

    // ---------------------------------------------------------------- cena 3
    private void demoBackground(float w, float h) {
        shapeRenderer.setColor(0.07f, 0.09f, 0.13f, 1f);
        fill(-60, -60, w + 120, h + 120);
        shapeRenderer.setColor(0.10f, 0.12f, 0.18f, 1f);
        for (float x = 0; x < w; x += ps * 8f) {
            fill(x, AREA_BOTTOM, 1, h - 60f - AREA_BOTTOM);
        }
        for (float y = AREA_BOTTOM; y < h - 60f; y += ps * 8f) {
            fill(0, y, w, 1);
        }
    }

    private void sceneNumbers(float w, float h) {
        demoBackground(w, h);
        int rows = 3;
        float cs = ps * 9f;
        layoutDemo(5, rows, cs, w * 0.5f, areaMidY(h) + ps * 2f);
        float loop = 7f;
        float ph = sceneTime % loop;
        boolean opened = ph >= 1.6f;
        if (crossed(loop, 1.6f)) {
            pop(tcx(2), tcy(1, rows), Color.CYAN, 14);
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 5; c++) {
                drawTile(tx(c), ty(r, rows), cs, (opened && r == 1 && c == 2) ? 1 : 0);
            }
        }
        if (opened) {
            float pop = MathUtils.clamp(1f - (ph - 1.6f) / 0.4f, 0f, 1f);
            drawDigit(countAround(demo1, 1, 2), tcx(2), tcy(1, rows), cs / 9f * (1f + pop * 0.5f),
                NUM_COLORS[countAround(demo1, 1, 2) - 1]);
        }
        if (ph >= 2.0f && ph < 6.6f) {
            float a = 0.6f + 0.4f * MathUtils.sin(animTime * 8f);
            drawFrame(tx(1) - 3f, ty(2, rows) - 3f, demoPitch * 3f + 3f, demoPitch * 3f + 3f, 3f, Color.CYAN, a);
        }
        if (ph >= 2.4f && ph < 6.6f) {
            float a = 0.7f + 0.3f * MathUtils.sin(animTime * 6f);
            bombInTile(0, 3, rows, a);
            bombInTile(2, 3, rows, a);
            queueTextCentered(font, "= 2 CARGAS AO REDOR", w * 0.5f, demoY - 16f, Color.GOLD);
        }
        float startX = tcx(0) - demoPitch * 1.6f;
        float rx;
        if (ph < 0.3f) {
            rx = startX;
        } else if (ph < 1.3f) {
            rx = MathUtils.lerp(startX, tcx(1), smooth(0.3f, 1.3f, ph));
        } else if (ph < 6.2f) {
            rx = tcx(1);
        } else {
            rx = MathUtils.lerp(tcx(1), startX, smooth(6.2f, 7f, ph));
        }
        boolean moving = (ph > 0.3f && ph < 1.3f) || ph > 6.2f;
        drawRobot(rx, ty(1, rows) + 2f, robotPx(), Color.CYAN, moving ? 1 + ((int) (animTime * 8f)) % 2 : 0);
    }

    // ---------------------------------------------------------------- cena 4
    private void sceneFlags(float w, float h) {
        demoBackground(w, h);
        int rows = 3;
        float cs = ps * 9f;
        layoutDemo(5, rows, cs, w * 0.5f, areaMidY(h) + ps * 2f);
        float loop = 8f;
        float ph = sceneTime % loop;
        boolean flagged = ph >= 3.4f;
        if (crossed(loop, 3.4f)) {
            pop(tcx(3), tcy(1, rows), Color.GOLD, 20);
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 5; c++) {
                boolean isTarget = (r == 1 && c == 3);
                drawTile(tx(c), ty(r, rows), cs, isTarget ? (flagged ? 2 : 0) : 1);
                if (!isTarget) {
                    int n = countAround(demo2, r, c);
                    if (n > 0) {
                        drawDigit(n, tcx(c), tcy(r, rows), cs / 9f, NUM_COLORS[n - 1]);
                    }
                }
            }
        }
        if (ph >= 1.0f && ph < 3.4f) {
            float a = 0.6f + 0.4f * MathUtils.sin(animTime * 8f);
            drawFrame(tx(2) - 3f, ty(2, rows) - 3f, demoPitch * 3f + 3f, demoPitch * 3f + 3f, 3f, Color.CYAN, a);
            queueTextCentered(font, "SO RESTA 1 CASA FECHADA...", w * 0.5f, demoY - 16f, Color.LIGHT_GRAY);
        }
        if (flagged) {
            float k = MathUtils.clamp((ph - 3.4f) / 0.5f, 0f, 1f);
            float sc = k < 0.6f ? k / 0.6f * 1.3f : 1.3f - (k - 0.6f) / 0.4f * 0.3f;
            int px = Math.max(2, Math.round(cs / 12f * sc));
            drawSprite(FLAG, tcx(3), tcy(1, rows) - FLAG.length * px * 0.5f, px);
            queueTextCentered(font, "BANDEIRA! ELA E CARGA.", w * 0.5f, demoY - 16f, Color.GOLD);
        }
        float startX = tcx(0) - demoPitch * 1.6f;
        float endX = tcx(4) + demoPitch * 1.6f;
        float rx;
        if (ph < 2.4f) {
            rx = startX;
        } else if (ph < 3.3f) {
            rx = MathUtils.lerp(startX, tcx(2), smooth(2.4f, 3.3f, ph));
        } else if (ph < 3.9f) {
            rx = tcx(2);
        } else {
            rx = MathUtils.lerp(tcx(2), endX, smooth(3.9f, 5f, ph));
        }
        boolean moving = (ph > 2.4f && ph < 3.3f) || (ph > 3.9f && ph < 5f);
        if (ph < 7.2f) {
            drawRobot(rx, ty(1, rows) + 2f, robotPx(), Color.GOLD, moving ? 1 + ((int) (animTime * 8f)) % 2 : 0);
        }
    }

    // ---------------------------------------------------------------- cena 5
    private void sceneCascade(float w, float h) {
        demoBackground(w, h);
        int rows = 4;
        int cols = 8;
        float cs = ps * 7f;
        layoutDemo(cols, rows, cs, w * 0.5f, areaMidY(h) + ps * 2f);
        float loop = 10f;
        float ph = sceneTime % loop;
        float t0 = 1.5f;
        float step = 0.22f;
        if (crossed(loop, t0)) {
            pop(tcx(0), tcy(0, rows), Color.LIME, 12);
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int d = cascadeDist[r][c];
                boolean open = d >= 0 && ph >= t0 + d * step;
                drawTile(tx(c), ty(r, rows), cs, open ? 1 : 0);
                if (open) {
                    int n = countAround(demo3, r, c);
                    if (n > 0) {
                        drawDigit(n, tcx(c), tcy(r, rows), cs / 9f, NUM_COLORS[n - 1]);
                    }
                }
            }
        }
        float k = (ph - t0) / step;
        if (k > 0f && k < cascadeMax + 2) {
            float half = (k + 0.5f) * demoPitch;
            drawFrame(tcx(0) - half, tcy(0, rows) - half, half * 2f, half * 2f, 3f, Color.LIME, 0.5f);
        }
        if (ph > t0 + cascadeMax * step + 0.8f && ph < 9f) {
            float a = 0.5f + 0.4f * MathUtils.sin(animTime * 6f);
            int[][] mp = {{0, 6}, {2, 5}, {3, 2}};
            for (int[] m : mp) {
                bombInTile(m[0], m[1], rows, a);
            }
            queueTextCentered(font, "ZERO = VIZINHANCA SEGURA", w * 0.5f, demoY - 16f, Color.LIME);
        }
        float startX = tcx(0) - demoPitch * 1.8f;
        float rx = ph < 0.2f ? startX : MathUtils.lerp(startX, tcx(0) - demoPitch * 0.9f, smooth(0.2f, 1.1f, ph));
        if (ph > 1.1f) {
            rx = tcx(0) - demoPitch * 0.9f;
        }
        if (ph < 9f) {
            drawRobot(rx, ty(0, rows) + 2f, robotPx(), Color.LIME, (ph > 0.2f && ph < 1.1f) ? 1 + ((int) (animTime * 8f)) % 2 : 0);
        }
    }

    // ---------------------------------------------------------------- cena 6
    private void drawMouse(float cx, float cy, boolean left, Color acc) {
        float bw = ps * 6f;
        float bh = ps * 9f;
        float x = cx - bw * 0.5f;
        float y = cy - bh * 0.5f;
        shapeRenderer.setColor(0.07f, 0.09f, 0.17f, 1f);
        fill(x - ps, y - ps, bw + ps * 2f, bh + ps * 2f);
        shapeRenderer.setColor(0.62f, 0.68f, 0.78f, 1f);
        fill(x, y, bw, bh);
        float by = y + bh * 0.55f;
        shapeRenderer.setColor(left ? acc.r : 0.80f, left ? acc.g : 0.84f, left ? acc.b : 0.90f, 1f);
        fill(x, by, bw * 0.5f - ps * 0.5f, bh * 0.45f);
        shapeRenderer.setColor(left ? 0.80f : acc.r, left ? 0.84f : acc.g, left ? 0.90f : acc.b, 1f);
        fill(x + bw * 0.5f + ps * 0.5f, by, bw * 0.5f - ps * 0.5f, bh * 0.45f);
        shapeRenderer.setColor(0.07f, 0.09f, 0.17f, 1f);
        fill(x + bw * 0.5f - ps * 0.5f, by, ps, bh * 0.45f);
        fill(x, by - ps * 0.5f, bw, ps * 0.5f);
    }

    private void drawKey(float cx, float cy, float kw, float kh, String label, Color acc) {
        shapeRenderer.setColor(0.07f, 0.09f, 0.17f, 1f);
        fill(cx - kw * 0.5f - ps, cy - kh * 0.5f - ps, kw + ps * 2f, kh + ps * 2f);
        shapeRenderer.setColor(0.62f, 0.68f, 0.78f, 1f);
        fill(cx - kw * 0.5f, cy - kh * 0.5f, kw, kh);
        shapeRenderer.setColor(0.80f, 0.84f, 0.90f, 1f);
        fill(cx - kw * 0.5f, cy + kh * 0.5f - ps, kw, ps);
        shapeRenderer.setColor(acc.r, acc.g, acc.b, 1f);
        fill(cx - kw * 0.5f, cy - kh * 0.5f, kw, ps * 0.8f);
        queueTextCentered(font, label, cx, cy + 9f, Color.BLACK);
    }

    private void sceneControls(float w, float h) {
        demoBackground(w, h);
        float mid = areaMidY(h);
        float gap = ps * 13f;
        float ix = w * 0.14f;
        float lx = ix + ps * 9f;
        float y0 = mid + gap * 1.5f;
        drawMouse(ix, y0, true, Color.CYAN);
        queueText(font, "CLIQUE ESQUERDO: ABRIR", lx, y0 + 9f, Color.WHITE);
        drawMouse(ix, y0 - gap, false, Color.GOLD);
        queueText(font, "CLIQUE DIREITO: BANDEIRA", lx, y0 - gap + 9f, Color.WHITE);
        drawKey(ix, y0 - gap * 2f, ps * 12f, ps * 6f, "ESPACO", Color.LIME);
        queueText(font, "RC-8 DEDUZ E JOGA", lx + ps * 3f, y0 - gap * 2f + 9f, Color.WHITE);
        drawKey(ix, y0 - gap * 3f, ps * 6f, ps * 6f, "N", Color.ORANGE);
        queueText(font, "NOVO SETOR", lx, y0 - gap * 3f + 9f, Color.WHITE);

        float cx = w * 0.76f;
        float loop = 4.5f;
        float ph = sceneTime % loop;
        float bottom = mid - ps * 6f;
        if (crossed(loop, 2.4f)) {
            pop(cx, bottom + ps * 6f, Color.ORANGE, 30);
            pop(cx, bottom + ps * 6f, Color.YELLOW, 20);
            shakeKick = 9f;
        }
        if (ph < 2.4f) {
            float wob = ph > 1.5f ? (((int) (animTime * 30f)) % 2 == 0 ? ps * 0.6f : -ps * 0.6f) : 0f;
            drawBomb(cx + wob, bottom, ps * 2, 1f);
            queueTextCentered(font, "NAO ABRA UMA CARGA!", cx, bottom - ps * 4f, Color.SALMON);
        } else {
            float t = ph - 2.4f;
            if (t < 1.2f) {
                float rad = smooth(0f, 0.5f, t) * ps * 22f;
                float a = 1f - t / 1.2f;
                float cy = bottom + ps * 6f;
                shapeRenderer.setColor(0.85f, 0.12f, 0.05f, 0.9f * a);
                pixelCircle(cx, cy, rad, ps);
                shapeRenderer.setColor(1f, 0.55f, 0.05f, 0.9f * a);
                pixelCircle(cx, cy, rad * 0.7f, ps);
                shapeRenderer.setColor(1f, 0.95f, 0.6f, a);
                pixelCircle(cx, cy, rad * 0.4f, ps);
            }
            if (t > 0.1f && t < 1.6f) {
                queueTextCentered(bigFont, "BOOM!", cx, bottom + ps * 30f, Color.RED);
            }
            if (t > 1.8f) {
                drawBomb(cx, bottom, ps * 2, smooth(1.8f, 2.1f, t));
            }
        }
    }

    // ---------------------------------------------------------------- cena 7
    private void sceneFinale(float w, float h) {
        float tt = sceneTime;
        drawNightSky(w, h, 0f);
        drawGroundAndHills(w, 0f);
        float gate = MathUtils.clamp((tt - 3.8f) / 1f, 0f, 1f);
        drawFactory(w, false, gate);

        float target = w * 0.5f;
        float speed = w * 0.5f / 4f;
        float x = Math.min(target, -ps * 8f + tt * speed);
        boolean walking = x < target;
        int frame = walking ? 1 + ((int) (tt * 6f)) % 2 : 0;
        drawRobot(x, GROUND_Y - ps * 4f, ps, Color.GOLD, frame);

        if (tt > 1.0f) {
            queueTextCentered(bigFont, "PROTOCOLO MINADO", w * 0.5f + 3f, h - 112f - 3f, Color.BLACK);
            queueTextCentered(bigFont, "PROTOCOLO MINADO", w * 0.5f, h - 112f, Color.GOLD);
        }
        if (tt > 1.8f) {
            queueTextCentered(font, "RC-8 // MISSAO: LIMPAR O SETOR", w * 0.5f, h - 170f, Color.LIGHT_GRAY);
        }
    }

    // ---------------------------------------------------------------- UI
    private void drawUi(float w, float h) {
        Color acc = ACCENTS[currentScene];

        shapeRenderer.setColor(0f, 0f, 0f, 0.55f);
        fill(-60, h - 58, w + 120, 70);
        shapeRenderer.setColor(acc.r, acc.g, acc.b, 1f);
        fill(-60, h - 60, w + 120, 3);
        queueText(titleFont, TITLES[currentScene], 24f, h - 16f, acc);

        for (int i = 0; i < TOTAL_SLIDES; i++) {
            boolean cur = i == currentScene;
            float size = cur ? ps * 2f : ps * 1.2f;
            float dx = w - 24f - (TOTAL_SLIDES - i) * ps * 3.5f;
            if (cur) {
                shapeRenderer.setColor(acc.r, acc.g, acc.b, 1f);
            } else {
                shapeRenderer.setColor(0.30f, 0.35f, 0.44f, 1f);
            }
            fill(dx, h - 32f - size * 0.5f, size, size);
        }

        float bx = 20f;
        float by = 18f;
        float bw = w - 40f;
        float bh = 130f;
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        fill(bx + 4f, by - 4f, bw, bh);
        shapeRenderer.setColor(acc.r, acc.g, acc.b, 1f);
        fill(bx - 3f, by - 3f, bw + 6f, bh + 6f);
        shapeRenderer.setColor(0.07f, 0.09f, 0.14f, 0.97f);
        fill(bx, by, bw, bh);

        layout.setText(font, SPEAKERS[currentScene]);
        float tagW = layout.width + 28f;
        shapeRenderer.setColor(acc.r, acc.g, acc.b, 1f);
        fill(bx + 14f, by + bh - 2f, tagW, 26f);
        queueText(font, SPEAKERS[currentScene], bx + 28f, by + bh + 20f, 0.05f, 0.06f, 0.10f, 1f);

        boolean done = charsShown >= TEXTS[currentScene].length();
        if (done && transState == 0 && ((int) (animTime * 3f)) % 2 == 0) {
            shapeRenderer.setColor(acc.r, acc.g, acc.b, 1f);
            shapeRenderer.triangle(bx + bw - 40f, by + 26f, bx + bw - 22f, by + 26f, bx + bw - 31f, by + 12f);
        }
        queueText(font, "[ESPACO] avancar | [<-] voltar | [ESC] pular", bx + 20f, by + 24f, 0.45f, 0.50f, 0.58f, 1f);
    }

    private void drawTransition(float w, float h) {
        float bs = 48f;
        int nx = (int) Math.ceil(w / bs) + 1;
        int ny = (int) Math.ceil(h / bs) + 1;
        shapeRenderer.setColor(0.03f, 0.04f, 0.07f, 1f);
        for (int bx = 0; bx < nx; bx++) {
            for (int by = 0; by < ny; by++) {
                float thr = (bx + by) / (float) (nx + ny - 2);
                float f = MathUtils.clamp((transT - thr * 0.6f) / 0.4f, 0f, 1f);
                if (f > 0f) {
                    float s = bs * f;
                    fill(bx * bs + (bs - s) * 0.5f, by * bs + (bs - s) * 0.5f, s + 0.5f, s + 0.5f);
                }
            }
        }
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
        if (bigFont != null) bigFont.dispose();
    }
}
