package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wiki / Bestiary menu.
 *  - 3 cards ang kita, isang card kada scroll (smooth).
 *  - Lahat locked ("???") hanggang ma-unlock via WikiProgress.unlock(id).
 *  - Input: arrow buttons (mouse), mouse wheel, at keys galing sa GameControls (arrows/WASD/Enter/ESC o custom).
 *  - Click / SELECT sa unlocked card -> LoreScreen.
 */
public class WikiMenuScreen implements Screen {

    // ====== SETTINGS (1920 x 1080) ======
    private static final float W = 1920, H = 1080;
    private static final float CARD_W = 480, CARD_H = 648, GAP = 56;
    private static final float CARD_Y = 160;           // ayon sa mockup
    private static final float HEADER_H = 165;
    private static final float ARROW_W = 172, ARROW_H = 200; // native size ng PNG (may padding)
    private static final float ARROW_LEFT_X = 1, ARROW_RIGHT_X = 1747, ARROW_Y = 430;
    private static final int VISIBLE = 3;
    private static final float LIFT = 18f;

    /** Para sa testing lang: F1 = unlock all, F2 = lock all. Gawing false sa release. */
    private static final boolean DEBUG_KEYS = false;

    /** Para sa testing lang: double click sa locked card = i-unlock agad (saved). Gawing false sa release. */
    private static final boolean DEBUG_DOUBLE_CLICK_UNLOCK = true;

    // ====== STATE ======
    private final Game game;
    private final Stage stage = new Stage(new FitViewport(W, H));
    private final Map<String, Texture> textures = new HashMap<>();
    private final List<WikiCreature> list = WikiCreature.ALL;
    private final int count = list.size();
    private final int maxTarget = Math.max(0, count - VISIBLE);
    private final Image[] cards = new Image[count];
    private final boolean[] unlocked = new boolean[count];
    private final float[] lift = new float[count];
    private final long[] lastClick = new long[count];
    private Image leftArrow, rightArrow;

    private int target;        // unang card ng window na pupuntahan
    private int focus;         // piniling card (keyboard / mouse)
    private float scrollPos;   // kasalukuyang (animated) posisyon ng window
    private boolean disposed;

    public WikiMenuScreen(Game game) { this(game, 0); }

    /** @param focusIndex balik dito galing LoreScreen para parehong card ang naka-focus */
    public WikiMenuScreen(Game game, int focusIndex) {
        this.game = game;
        this.focus = MathUtils.clamp(focusIndex, 0, count - 1);
        this.target = MathUtils.clamp(focus - (VISIBLE - 1), 0, maxTarget);
        this.scrollPos = target;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (GameControls.is(GameControls.Action.BACK, keycode)) {
                    AudioManager.getInstance().playBtnDeselect();
                    goTo(new StartMenuScreen(game));
                    return true;
                }
                if (GameControls.is(GameControls.Action.LEFT, keycode))  { moveFocus(-1); return true; }
                if (GameControls.is(GameControls.Action.RIGHT, keycode)) { moveFocus(1);  return true; }
                if (GameControls.is(GameControls.Action.SELECT, keycode)) { open(focus);  return true; }

                if (DEBUG_KEYS && keycode == Input.Keys.F1) { WikiProgress.unlockAll(); goTo(new WikiMenuScreen(game, focus)); return true; }
                if (DEBUG_KEYS && keycode == Input.Keys.F2) { WikiProgress.resetAll();  goTo(new WikiMenuScreen(game, focus)); return true; }
                return false;
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                float a = Math.abs(amountY) > Math.abs(amountX) ? amountY : amountX;
                if (a > 0) scrollWindow(1);
                else if (a < 0) scrollWindow(-1);
                return true;
            }
        });

        // 1. Background (kasama na ang itim na linya)
        Image bg = new Image(tex("wiki/background.png", true));
        bg.setSize(W, H);
        stage.addActor(bg);

        // 2. Cards
        for (int i = 0; i < count; i++) {
            final int idx = i;
            WikiCreature c = list.get(i);
            unlocked[i] = WikiProgress.isUnlocked(c.id);

            Image card = new Image(tex(unlocked[i] ? c.cardFile : "wiki/card_locked.png", true));
            card.setSize(CARD_W, CARD_H);
            card.addListener(new ClickListener() {
                // mouseMoved (hindi enter) para hindi maagaw ng nakatigil na mouse ang keyboard focus
                @Override
                public boolean mouseMoved(InputEvent event, float x, float y) {
                    if (focus != idx) {
                        focus = idx;
                        AudioManager.getInstance().playWikiCardHover();
                    }
                    return false;
                }
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    focus = idx;
                    if (!unlocked[idx]) {
                        AudioManager.getInstance().playBtnDeselect();
                        // TEST: double click sa locked card = i-unlock
                        long now = TimeUtils.millis();
                        Gdx.app.log("Wiki", "click locked card " + idx + " (debug=" + DEBUG_DOUBLE_CLICK_UNLOCK + ")");
                        if (DEBUG_DOUBLE_CLICK_UNLOCK && now - lastClick[idx] <= 600) {
                            lastClick[idx] = 0;
                            debugUnlock(idx);
                        } else {
                            lastClick[idx] = now;
                        }
                        return;
                    }
                    open(idx);
                }
            });
            cards[i] = card;
            stage.addActor(card);
        }

        // 3. Header (itim na linya ay nasa background; transparent ang header)
        Image header = new Image(tex("wiki/header.png", true));
        header.setSize(W, HEADER_H);
        header.setPosition(0, H - HEADER_H);
        header.setTouchable(Touchable.disabled);
        stage.addActor(header);

        // 4. Arrows
        leftArrow = makeArrow("wiki/arrow_left.png", ARROW_LEFT_X, -1);
        rightArrow = makeArrow("wiki/arrow_right.png", ARROW_RIGHT_X, 1);
        stage.addActor(leftArrow);
        stage.addActor(rightArrow);

        updateCards(1f); // i-place agad bago ang unang frame
    }

    // ---------- Navigation ----------

    /** Keyboard: galaw ng focus; sumusunod ang window. */
    private void moveFocus(int dir) {
        int oldFocus = focus;
        focus = MathUtils.clamp(focus + dir, 0, count - 1);
        if (focus != oldFocus) {
            AudioManager.getInstance().playWikiCardHover();
        }
        if (focus < target) target = focus;
        else if (focus > target + VISIBLE - 1) target = focus - (VISIBLE - 1);
    }

    /** Arrow buttons / mouse wheel: isang card ang usad ng window. */
    private void scrollWindow(int dir) {
        int oldTarget = target;
        target = MathUtils.clamp(target + dir, 0, maxTarget);
        focus = Math.min(MathUtils.clamp(focus, target, target + VISIBLE - 1), count - 1);
        if (target != oldTarget) {
            AudioManager.getInstance().playWikiCardHover();
        }
    }

    private void open(int idx) {
        if (idx < 0 || idx >= count) return;
        if (!unlocked[idx]) {
            AudioManager.getInstance().playBtnDeselect();
            if (DEBUG_DOUBLE_CLICK_UNLOCK) debugUnlock(idx); // TEST: Enter sa locked card = i-unlock
            return;
        }
        AudioManager.getInstance().playWikiCardSelect();
        goTo(new LoreScreen(game, idx));
    }

    /** TEST ONLY: i-unlock ang card at palitan agad ang itsura (hindi na kailangan mag-reload). */
    private void debugUnlock(int idx) {
        WikiCreature c = list.get(idx);
        WikiProgress.unlock(c.id);
        unlocked[idx] = true;
        cards[idx].setDrawable(new TextureRegionDrawable(new TextureRegion(tex(c.cardFile, true))));
    }

    private void goTo(Screen next) {
        game.setScreen(next);
        Gdx.app.postRunnable(this::dispose); // ligtas na mag-dispose pagkatapos ng event
    }

    private Image makeArrow(String file, float x, final int dir) {
        Image arrow = new Image(tex(file, false)); // pixel art: Nearest
        arrow.setSize(ARROW_W, ARROW_H);
        arrow.setPosition(x, ARROW_Y);
        arrow.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                scrollWindow(dir);
            }
        });
        return arrow;
    }

    // ---------- Per-frame layout ----------

    private void updateCards(float dt) {
        scrollPos += (target - scrollPos) * Math.min(1f, dt * 14f);
        if (Math.abs(target - scrollPos) < 0.002f) scrollPos = target;

        float step = CARD_W + GAP;
        float startX = (W - (VISIBLE * CARD_W + (VISIBLE - 1) * GAP)) / 2f;

        for (int i = 0; i < count; i++) {
            float pos = i - scrollPos;
            float out = Math.max(0f, Math.max(-pos, pos - (VISIBLE - 1))); // layo sa window
            float a = MathUtils.clamp(1f - out * 1.5f, 0f, 1f);

            boolean f = (i == focus);
            float goal = f ? (unlocked[i] ? LIFT : LIFT * 0.4f) : 0f;
            lift[i] += (goal - lift[i]) * Math.min(1f, dt * 18f);

            float t = f ? 1f : 0.8f; // medyo madilim ang hindi naka-focus
            Image card = cards[i];
            card.setPosition(startX + pos * step, CARD_Y + lift[i]);
            card.setColor(t, t, t, a);
            card.setTouchable(a > 0.6f ? Touchable.enabled : Touchable.disabled);
        }

        leftArrow.getColor().a = target > 0 ? 1f : 0.3f;
        rightArrow.getColor().a = target < maxTarget ? 1f : 0.3f;
    }

    // ---------- Texture loader ----------
    private Texture tex(String file, boolean smooth) {
        Texture t = textures.get(file);
        if (t != null) return t;
        if (smooth) {
            t = new Texture(Gdx.files.internal(file), true);
            t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        } else {
            t = new Texture(Gdx.files.internal(file));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        textures.put(file, t);
        return t;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        float dt = Math.min(delta, 1f / 30f);
        stage.act(dt);
        updateCards(dt);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        stage.dispose();
        for (Texture t : textures.values()) t.dispose();
    }
}
