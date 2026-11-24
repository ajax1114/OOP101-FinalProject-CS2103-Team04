package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;

public class DungeonBound extends Game {

    private static final String PREFS_NAME = "dungeonbound_settings";
    private static final String PREF_LANG = "language";

    private Ui ui;

    public Ui getUi() {
        if (ui == null) {
            ui = new Ui();
        }
        return ui;
    }

    public Lang getSelectedLanguage() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String code = prefs.getString(PREF_LANG, "TL");
        return "EN".equalsIgnoreCase(code) ? Lang.EN : Lang.TL;
    }

    public void setLanguage(Lang lang) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(PREF_LANG, lang == Lang.EN ? "EN" : "TL");
        prefs.flush();
    }

    @Override
    public void create() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        if (!prefs.contains(PREF_LANG)) {
            // First time open: show Language Selection
            setScreen(new LanguageSelectScreen(getUi(), picked -> {
                setLanguage(picked);
                setScreen(new TitleScreen(this));
            }));
        } else {
            // Returning player: go straight to TitleScreen
            setScreen(new TitleScreen(this));
        }
    }

    // Free the previous screen's resources on every screen change, unless disposeOld is false.
    public void setScreen(Screen screen, boolean disposeOld) {
        Screen old = getScreen();
        super.setScreen(screen);
        if (old != null && disposeOld && old != screen) {
            Gdx.app.postRunnable(old::dispose); // after the frame, so an active transition can't crash
        }
    }

    @Override
    public void setScreen(Screen screen) {
        setScreen(screen, true);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (ui != null) {
            ui.dispose();
            ui = null;
        }
        AudioManager.getInstance().dispose();
    }
}
