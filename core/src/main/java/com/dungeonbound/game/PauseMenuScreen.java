package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Pause menu. Ginagamit ang buong menu_board.png (286x291, may 4 na scroll sa bawat gilid at fleur).
 * Lahat ng posisyon ay sinukat sa Pause_Menu_Screen_640x360.mp4.
 */
public final class PauseMenuScreen extends ScreenAdapter {
    private static final int BOARD_X = 177, BOARD_Y = 21;
    private static final int PAD_X = 234;          // kaliwang gilid ng divider at mga button
    private static final int DIVIDER_Y = 100, DIVIDER_W = 172;
    private static final int BTN_Y = 107, BTN_W = 170, BTN_H = 30, BTN_STEP = 36;

    private final Ui ui;
    private final Lang lang;
    private final Runnable[] actions; // kasing-dami ng lang.items; [0] = Ipagpatuloy
    private final PixelViewport view = new PixelViewport();
    private final MenuList list;

    public PauseMenuScreen(Ui ui, Lang lang, Runnable[] actions) {
        if (actions.length != lang.items.length) {
            throw new IllegalArgumentException("Dapat " + lang.items.length + " ang actions");
        }
        this.ui = ui;
        this.lang = lang;
        this.actions = actions;
        this.list = new MenuList(lang.items, PAD_X, BTN_Y, BTN_W, BTN_H, BTN_STEP);
    }

    /**
     * Gumawa ng pause menu na may gumaganang "Mga Setting" (index 3).
     * Pagbalik galing Settings, bagong PauseMenuScreen ang ipapakita (kaya updated ang wika).
     */
    public static PauseMenuScreen create(final DungeonBound game, final Runnable resume,
                                         final Runnable items, final Runnable lore,
                                         final Runnable backToStart) {
        Runnable[] actions = new Runnable[5];
        actions[0] = resume;
        actions[1] = items;
        actions[2] = lore;
        actions[3] = () -> SettingsScreen.open(game,
                () -> game.setScreen(create(game, resume, items, lore, backToStart)));
        actions[4] = backToStart;
        return new PauseMenuScreen(game.getUi(), Lang.get(), actions);
    }

    @Override
    public void show() {
        // Esc = Ipagpatuloy
        Gdx.input.setInputProcessor(new MenuInput(list, view, () -> actions[list.sel].run(), actions[0]));
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
        // Sa totoong laro: iguhit muna ang frozen na game, tapos black na may alpha 0.7 sa halip na solid BG.
        ScreenUtils.clear(Ui.BG);
        view.apply(true);
        ui.batch.setProjectionMatrix(view.getCamera().combined);
        ui.batch.begin();

        ui.batch.draw(ui.menuBoard, BOARD_X, Ui.up(BOARD_Y, ui.menuBoard.getHeight()));
        ui.text(ui.body, lang.title, PAD_X + 1, 89, Ui.CREAM);
        ui.textRight(ui.tag, lang.tag, PAD_X + DIVIDER_W - 1, 88, Ui.MUTE);
        ui.rect(PAD_X, DIVIDER_Y, DIVIDER_W, 1, Ui.EDGE);
        list.draw(ui);

        ui.batch.end();
    }
}
