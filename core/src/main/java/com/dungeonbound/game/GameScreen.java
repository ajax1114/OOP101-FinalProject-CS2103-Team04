package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Main gameplay screen for DungeonBound.
 * Includes camera follow, tilemap collisions, player animations, sword attacks,
 * and on-screen joystick + keyboard controls.
 * Features an integrated in-game Pause Menu overlay so unpausing never crashes or unloads assets.
 */
public class GameScreen implements Screen {

    private static final float VIEW_W = 400f; // how much of the dungeon is visible (world pixels)
    private static final float VIEW_H = 300f;

    private static final int BOARD_X = 177, BOARD_Y = 21;
    private static final int PAD_X = 234;
    private static final int DIVIDER_Y = 100, DIVIDER_W = 172;
    private static final int BTN_Y = 107, BTN_W = 170, BTN_H = 30, BTN_STEP = 36;

    private final Game game;
    private final AssetManager assets;

    private SpriteBatch batch;
    private ShapeRenderer shape;
    private GameMap map;
    private Player player;
    private Joystick joystick;

    private OrthographicCamera camera;
    private Viewport viewport;
    private OrthographicCamera hudCamera;
    private Viewport hudViewport;

    // Pause Menu Overlay
    private final PixelViewport pauseView = new PixelViewport();
    private MenuList pauseList;
    private MenuInput pauseInput;
    private boolean isPaused = false;
    private boolean isDisposed = false;

    public GameScreen(Game game, AssetManager assets) {
        this.game = game;
        this.assets = assets;
        init();
    }

    public GameScreen(Game game) {
        this(game, null);
    }

    private void init() {
        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        map = new GameMap();

        // Start in the center chamber
        player = new Player(640f, map.height - 330f);
        joystick = new Joystick();

        camera = new OrthographicCamera();
        viewport = new ExtendViewport(VIEW_W, VIEW_H, camera);
        hudCamera = new OrthographicCamera();
        hudViewport = new ScreenViewport(hudCamera);
    }

    private void setupPauseMenu() {
        Lang lang = Lang.get();
        pauseList = new MenuList(lang.items, PAD_X, BTN_Y, BTN_W, BTN_H, BTN_STEP);

        Runnable[] actions = new Runnable[5];

        // 0: Ipagpatuloy / Resume (bumalik sa laro)
        actions[0] = () -> {
            isPaused = false;
            Gdx.input.setInputProcessor(null);
        };

        // 1: Mga Gamit / Items (placeholder)
        actions[1] = () -> {};

        // 2: Talaan ng Nilalang / Bestiary
        actions[2] = () -> {
            if (game instanceof DungeonBound) {
                ((DungeonBound) game).setScreen(new WikiMenuScreen(game), false);
            } else {
                game.setScreen(new WikiMenuScreen(game));
            }
        };

        // 3: Mga Setting / Settings
        actions[3] = () -> {
            if (game instanceof DungeonBound) {
                SettingsScreen.open((DungeonBound) game, () -> {
                    ((DungeonBound) game).setScreen(this, false);
                    isPaused = true;
                    setupPauseMenu();
                    Gdx.input.setInputProcessor(pauseInput);
                });
            }
        };

        // 4: Bumalik sa Simula / Back to Start
        actions[4] = () -> {
            dispose();
            game.setScreen(new StartMenuScreen(game));
        };

        pauseInput = new MenuInput(pauseList, pauseView, () -> actions[pauseList.sel].run(), actions[0]);
    }

    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            setupPauseMenu();
            Gdx.input.setInputProcessor(pauseInput);
        } else {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void show() {
        if (isPaused) {
            setupPauseMenu();
            Gdx.input.setInputProcessor(pauseInput);
        } else {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f);

        // ---- When NOT paused, process game controls ----
        if (!isPaused) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                togglePause();
                return;
            }

            // Input: Joystick (touch/mouse) + Keyboard (WASD / Arrows)
            Vector2 stick = joystick.update(hudViewport);
            float dx = stick.x;
            float dy = stick.y;

            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1f;

            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                if (!player.isAttacking()) {
                    player.attack();
                    AudioManager.getInstance().playSfx("audio/sfx_whoosh.wav", 0.5f);
                }
            }

            // Update player position and animation
            player.update(dt, dx, dy, map);

            // Camera follow (never show outside the map bounds)
            float halfW = viewport.getWorldWidth() / 2f;
            float halfH = viewport.getWorldHeight() / 2f;
            camera.position.x = map.width <= halfW * 2 ? map.width / 2f : MathUtils.clamp(player.x, halfW, map.width - halfW);
            camera.position.y = map.height <= halfH * 2 ? map.height / 2f : MathUtils.clamp(player.y + 10f, halfH, map.height - halfH);
            camera.update();
        }

        // ---- Draw world (lagi itong iginuguhit kahit paused para makita ang dungeon) ----
        ScreenUtils.clear(0.13f, 0.13f, 0.13f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        map.render(batch);
        player.render(batch);
        batch.end();

        // ---- HUD / Pause Overlay ----
        if (!isPaused) {
            hudViewport.apply();
            shape.setProjectionMatrix(hudCamera.combined);
            joystick.render(shape);
        } else {
            // Draw semi-transparent dark overlay over the whole screen
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shape.setProjectionMatrix(hudCamera.combined);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(0f, 0f, 0f, 0.65f);
            shape.rect(0, 0, hudViewport.getWorldWidth(), hudViewport.getWorldHeight());
            shape.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            // Draw Pause Menu Board
            if (game instanceof DungeonBound && pauseList != null) {
                Ui ui = ((DungeonBound) game).getUi();
                Lang lang = Lang.get();
                pauseView.apply(true);
                ui.batch.setProjectionMatrix(pauseView.getCamera().combined);
                ui.batch.begin();

                ui.batch.draw(ui.menuBoard, BOARD_X, Ui.up(BOARD_Y, ui.menuBoard.getHeight()));
                ui.text(ui.body, lang.title, PAD_X + 1, 89, Ui.CREAM);
                ui.textRight(ui.tag, lang.tag, PAD_X + DIVIDER_W - 1, 88, Ui.MUTE);
                ui.rect(PAD_X, DIVIDER_Y, DIVIDER_W, 1, Ui.EDGE);
                pauseList.draw(ui);

                ui.batch.end();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        hudViewport.update(width, height, true);
        pauseView.update(width, height, true);
        joystick.layout(hudViewport.getWorldHeight());
    }

    @Override
    public void pause() {
        if (!isPaused) {
            togglePause();
        }
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (isDisposed) return;
        isDisposed = true;
        if (batch != null) batch.dispose();
        if (shape != null) shape.dispose();
        if (map != null) map.dispose();
        if (player != null) player.dispose();
        if (assets != null) assets.dispose();
    }
}
