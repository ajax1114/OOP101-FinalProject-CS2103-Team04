package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;

/**
 * GameScreen – placeholder screen shown after loading completes.
 *
 * <p><b>TODO:</b> Replace with the actual dungeon gameplay once the
 * world/combat modules are ready. Currently displays a solid green screen
 * and returns to the main menu on tap or ESC.
 */
public class GameScreen implements Screen {

    private final Game game;
    private final AssetManager assets;

    public GameScreen(Game game, AssetManager assets) {
        this.game = game;
        this.assets = assets;
    }

    @Override public void show() {}

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.25f, 0.1f, 1f); // green tint – placeholder indicator
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Tap or ESC → return to main menu
        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            game.setScreen(new StartMenuScreen(game));
            dispose();
        }
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        assets.dispose();
    }
}