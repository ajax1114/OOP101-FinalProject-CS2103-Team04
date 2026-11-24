package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
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
 */
public class GameScreen implements Screen {

    private static final float VIEW_W = 400f; // how much of the dungeon is visible (world pixels)
    private static final float VIEW_H = 300f;

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

    private boolean isPaused = false;

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

    @Override
    public void show() {
        isPaused = false;
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f);

        // ---- ESC = Open Pause Menu ----
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            openPauseMenu();
            return;
        }

        // ---- Input: Joystick (touch/mouse) + Keyboard (WASD / Arrows) ----
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

        // ---- Update player position and animation ----
        player.update(dt, dx, dy, map);

        // ---- Camera follow (never show outside the map bounds) ----
        float halfW = viewport.getWorldWidth() / 2f;
        float halfH = viewport.getWorldHeight() / 2f;
        camera.position.x = map.width <= halfW * 2 ? map.width / 2f : MathUtils.clamp(player.x, halfW, map.width - halfW);
        camera.position.y = map.height <= halfH * 2 ? map.height / 2f : MathUtils.clamp(player.y + 10f, halfH, map.height - halfH);
        camera.update();

        // ---- Draw world ----
        ScreenUtils.clear(0.13f, 0.13f, 0.13f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        map.render(batch);
        player.render(batch);
        batch.end();

        // ---- Draw HUD (on-screen joystick) ----
        hudViewport.apply();
        shape.setProjectionMatrix(hudCamera.combined);
        joystick.render(shape);
    }

    private void openPauseMenu() {
        if (game instanceof DungeonBound) {
            DungeonBound db = (DungeonBound) game;
            game.setScreen(PauseMenuScreen.create(
                db,
                () -> game.setScreen(this), // Resume game
                () -> {}, // Items / Inventory placeholder
                () -> game.setScreen(new WikiMenuScreen(game)), // Bestiary
                () -> {
                    dispose();
                    game.setScreen(new StartMenuScreen(game)); // Quit to main menu
                }
            ));
        } else {
            game.setScreen(new StartMenuScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        hudViewport.update(width, height, true);
        joystick.layout(hudViewport.getWorldHeight());
    }

    @Override
    public void pause() {
        isPaused = true;
    }

    @Override
    public void resume() {
        isPaused = false;
    }

    @Override
    public void hide() {
        isPaused = true;
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shape != null) shape.dispose();
        if (map != null) map.dispose();
        if (player != null) player.dispose();
        if (assets != null) assets.dispose();
    }
}
