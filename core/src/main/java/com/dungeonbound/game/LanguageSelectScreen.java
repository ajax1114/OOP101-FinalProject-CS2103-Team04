package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.function.Consumer;

/** Dalawang magkahiwalay na board: sa taas (may fleur), mga pagpipilian sa ibaba. */
public final class LanguageSelectScreen extends ScreenAdapter {
    private static final int BH = 127;
    private static final int B1X = 170, B1Y = 53, B1W = 300;
    private static final int B2X = 214, B2Y = 193, B2W = 212;
    private static final float CONFIRM_DELAY = 0.4f; // para makita ang gintong parisukat bago lumipat

    private static final Lang[] OPTIONS = {Lang.TL, Lang.EN};

    private final Ui ui;
    private final Consumer<Lang> onPicked;
    private final PixelViewport view = new PixelViewport();
    private final MenuList list = new MenuList(new String[]{"Tagalog", "English"}, B2X + 20, B2Y + 22, B2W - 40, 30, 40);
    private float confirmTimer = -1f;

    public LanguageSelectScreen(Ui ui, Consumer<Lang> onPicked) {
        this.ui = ui;
        this.onPicked = onPicked;
    }

    private void confirm() {
        if (confirmTimer >= 0f) return; // nakumpirma na
        list.done = list.sel;
        confirmTimer = 0f;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new MenuInput(list, view, this::confirm, null));
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void resize(int width, int height) {
        view.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        if (confirmTimer >= 0f) {
            confirmTimer += delta;
            if (confirmTimer >= CONFIRM_DELAY) {
                confirmTimer = Float.NEGATIVE_INFINITY; // isang beses lang
                onPicked.accept(OPTIONS[list.done]);
                return;
            }
        }
        ScreenUtils.clear(Ui.BG);
        view.apply(true);
        ui.batch.setProjectionMatrix(view.getCamera().combined);
        ui.batch.begin();

        ui.board(B1X, B1Y, B1W, BH, true);
        ui.textCenter(ui.title, "Choose your language", B1X + B1W / 2, B1Y + 58, Ui.CREAM);
        ui.textCenter(ui.body, "Piliin ang iyong wika", B1X + B1W / 2, B1Y + 82, Ui.SUB);

        ui.board(B2X, B2Y, B2W, BH, false);
        list.draw(ui);

        ui.batch.end();
    }
}
