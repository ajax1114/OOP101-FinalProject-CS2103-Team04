package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import java.util.ArrayList;
import java.util.List;

/**
 * Sandaling Loading Screen kapag nagpapalit ng screen mode / resolution.
 * Gamit ang parehong visual layers at spinner gaya ng LoadingScreen bago mag-start.
 */
public class DisplayLoadingScreen implements Screen {

    private static final float MIN_W = 1280f;
    private static final float MIN_H = 720f;
    private static final float ART_W = 1080f;
    private static final float ART_H = 720f;
    private static final float TITLE_X = 236f, TITLE_TOP = 89f,  TITLE_W = 620f, TITLE_H = 441f;
    private static final float TEXT_X  = 479f, TEXT_TOP  = 592f, TEXT_W  = 138f, TEXT_H  = 38f;
    private static final float SPIN_X  = 517f, SPIN_TOP  = 533f, SPIN_S   = 48f;
    private static final float FIT_H   = 700f;
    private static final float MAX_SCALE = 1.6f;
    private static final float SPIN_STEP = 1f / 12f;

    private static final float APPLY_DELAY   = 0.35f; // i-apply ang display change kapag natakpan na ng loading bg ang screen
    private static final float MIN_SHOW_TIME = 1.25f; // sapat para makita ang 12-dot spinner at matapos ang window resize
    private static final float FADE_OUT_TIME = 0.30f;

    private static final String SFX_PATH = "audio/sfx_loading.ogg";
    private static final float BASE_SFX_VOL = 0.5f;

    private final DungeonBound game;
    private final Screen returnScreen;
    private final Runnable applyAction;
    private final Stage stage = new Stage(new ExtendViewport(MIN_W, MIN_H));
    private final List<Texture> textures = new ArrayList<>();

    private final Image bg;
    private final Image title;
    private final Image spinner;
    private final Image text;

    private float elapsed = 0f;
    private float fadeElapsed = 0f;
    private long sfxId = -1;
    private boolean applied = false;
    private boolean leaving = false;
    private boolean switchNow = false;
    private boolean disposed = false;

    public DisplayLoadingScreen(DungeonBound game, Screen returnScreen, Runnable applyAction) {
        this.game = game;
        this.returnScreen = returnScreen;
        this.applyAction = applyAction;

        bg = new Image(load("ui/loading_bg.png"));
        stage.addActor(bg);

        title = new Image(load("ui/loading_title.png"));
        title.setScaling(Scaling.fit);
        stage.addActor(title);

        spinner = new Image(load("ui/loading_spinner.png"));
        spinner.addAction(Actions.forever(Actions.sequence(
            Actions.delay(SPIN_STEP),
            new Action() {
                @Override
                public boolean act(float delta) {
                    actor.rotateBy(-30f);
                    return true;
                }
            })));
        stage.addActor(spinner);

        text = new Image(load(Lang.isTL() ? "ui/loading_text_tl.png" : "ui/loading_text.png"));
        stage.addActor(text);

        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        layout();
    }

    private void layout() {
        float ww = stage.getViewport().getWorldWidth();
        float wh = stage.getViewport().getWorldHeight();
        float sc = Math.min(wh / FIT_H, MAX_SCALE);

        bg.setSize(ww, wh);
        place(title, ww, wh, sc, TITLE_X, TITLE_TOP, TITLE_W, TITLE_H);
        place(spinner, ww, wh, sc, SPIN_X, SPIN_TOP, SPIN_S, SPIN_S);
        spinner.setOrigin(spinner.getWidth() / 2f, spinner.getHeight() / 2f);
        if (Lang.isTL()) {
            place(text, ww, wh, sc, 452.5f, 599f, 191f, 24f);
        } else {
            place(text, ww, wh, sc, TEXT_X, TEXT_TOP, TEXT_W, TEXT_H);
        }
    }

    private static void place(Actor a, float ww, float wh, float sc,
                              float artX, float artTop, float w, float h) {
        float x = ww / 2f + (artX - ART_W / 2f) * sc;
        float top = wh / 2f + (artTop - ART_H / 2f) * sc;
        a.setSize(w * sc, h * sc);
        a.setPosition(x, wh - top - a.getHeight());
    }

    private Texture load(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(t);
        return t;
    }

    @Override
    public void show() {
        // Harangin ang accidental clicks habang naglo-load
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyDown(int keycode) { return true; }
            @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return true; }
        });
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.25f));
        sfxId = AudioManager.getInstance().playSfxId(SFX_PATH, BASE_SFX_VOL);
    }

    @Override
    public void render(float delta) {
        elapsed += delta;

        // Kapag natakpan na ng loading background ang screen, i-apply ang display change
        if (!applied && elapsed >= APPLY_DELAY) {
            applied = true;
            if (applyAction != null) {
                applyAction.run();
            }
        }

        Gdx.gl.glClearColor(25f / 255f, 25f / 255f, 25f / 255f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(delta, 1f / 30f));
        stage.draw();

        if (applied && !leaving && elapsed >= MIN_SHOW_TIME) {
            leaving = true;
            stage.getRoot().addAction(Actions.sequence(
                Actions.fadeOut(FADE_OUT_TIME),
                Actions.run(() -> switchNow = true)
            ));
        }

        // I-fade out ang SFX habang nagfe-fade out ang visual loading screen
        if (leaving && sfxId != -1) {
            fadeElapsed += delta;
            float progress = Math.min(1f, fadeElapsed / FADE_OUT_TIME);
            AudioManager.getInstance().setSfxSoundVolume(SFX_PATH, sfxId, BASE_SFX_VOL * (1f - progress));
            if (progress >= 1f) {
                AudioManager.getInstance().stopSfx(SFX_PATH, sfxId);
                sfxId = -1;
            }
        }

        if (switchNow) {
            switchNow = false;
            stopSound();
            if (returnScreen != null) {
                game.setScreen(returnScreen, false);
            }
            dispose();
        }
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
        layout();
    }

    private void stopSound() {
        if (sfxId != -1) {
            AudioManager.getInstance().stopSfx(SFX_PATH, sfxId);
            sfxId = -1;
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        stopSound();
        if (Gdx.input.getInputProcessor() != null) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        stopSound();
        stage.dispose();
        for (Texture t : textures) {
            t.dispose();
        }
        textures.clear();
    }
}
