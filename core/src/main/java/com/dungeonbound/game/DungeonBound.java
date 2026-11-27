package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

import java.util.HashMap;
import java.util.Map;

public class DungeonBound extends Game {

    public enum DisplayModeOption {
        FULLSCREEN, WINDOWED
    }

    public static final int[][] WINDOW_RESOLUTIONS = {
            {960, 540},
            {1280, 720},
            {1600, 900}
    };

    private static final String PREFS_NAME = "dungeonbound_settings";
    private static final String PREF_LANG = "language";
    private static final String PREF_DISPLAY_MODE = "display_mode";
    private static final String PREF_RES_INDEX = "window_res_index";
    private static final String PREF_VSYNC = "vsync";
    private static final String WIKI_ATLAS = "wiki/wiki.atlas";
    private static final String WIKI_BG = "wiki/background.png";
    private static final String WIKI_HEADER = "wiki/header.png";
    private static final int[] WIKI_FONT_SIZES = {40, 64, 28, 26, 24, 32, 30, 22};

    private Ui ui;
    private Preferences prefs;
    private AssetManager wikiAssets;
    private boolean wikiPreloadStarted = false;
    private boolean wikiSfxPreloaded = false;
    private int wikiFontPreloadIndex = 0;
    private final Map<Integer, BitmapFont> wikiFonts = new HashMap<>();

    public Ui getUi() {
        if (ui == null) {
            ui = new Ui();
        }
        return ui;
    }

    public void startWikiPreload() {
        if (wikiPreloadStarted) return;
        wikiPreloadStarted = true;
        wikiAssets = new AssetManager();
        TextureLoader.TextureParameter linear = new TextureLoader.TextureParameter();
        linear.minFilter = Texture.TextureFilter.Linear;
        linear.magFilter = Texture.TextureFilter.Linear;
        wikiAssets.load(WIKI_ATLAS, TextureAtlas.class);
        wikiAssets.load(WIKI_BG, Texture.class, linear);
        wikiAssets.load(WIKI_HEADER, Texture.class, linear);
    }

    private void stepWikiPreload() {
        if (!wikiPreloadStarted || wikiAssets == null) return;
        if (!wikiAssets.update(2)) return;
        if (!wikiSfxPreloaded) {
            wikiSfxPreloaded = true;
            AudioManager.getInstance().preloadSfx("audio/sfx_wiki_card_hover.ogg");
            AudioManager.getInstance().preloadSfx("audio/sfx_wiki_card_select.ogg");
            return;
        }
        if (wikiFontPreloadIndex < WIKI_FONT_SIZES.length) {
            getWikiFont(WIKI_FONT_SIZES[wikiFontPreloadIndex++]);
        }
    }

    public TextureAtlas getWikiAtlas() {
        startWikiPreload();
        if (!wikiAssets.isLoaded(WIKI_ATLAS, TextureAtlas.class)) {
            wikiAssets.finishLoadingAsset(WIKI_ATLAS);
        }
        return wikiAssets.get(WIKI_ATLAS, TextureAtlas.class);
    }

    public Texture getWikiBg() {
        startWikiPreload();
        if (!wikiAssets.isLoaded(WIKI_BG, Texture.class)) {
            wikiAssets.finishLoadingAsset(WIKI_BG);
        }
        return wikiAssets.get(WIKI_BG, Texture.class);
    }

    public Texture getWikiHeader() {
        startWikiPreload();
        if (!wikiAssets.isLoaded(WIKI_HEADER, Texture.class)) {
            wikiAssets.finishLoadingAsset(WIKI_HEADER);
        }
        return wikiAssets.get(WIKI_HEADER, Texture.class);
    }

    public BitmapFont getWikiFont(int size) {
        BitmapFont cached = wikiFonts.get(size);
        if (cached != null) return cached;

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/font_body.ttf"));
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = size;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
        BitmapFont f = gen.generateFont(p);
        gen.dispose();
        wikiFonts.put(size, f);
        return f;
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

    private Preferences prefs() {
        if (prefs == null) {
            prefs = Gdx.app.getPreferences(PREFS_NAME);
        }
        return prefs;
    }

    public DisplayModeOption getDisplayMode() {
        String raw = prefs().getString(PREF_DISPLAY_MODE, DisplayModeOption.FULLSCREEN.name());
        try {
            return DisplayModeOption.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return DisplayModeOption.FULLSCREEN;
        }
    }

    public void setDisplayMode(DisplayModeOption mode) {
        prefs().putString(PREF_DISPLAY_MODE, mode.name());
        prefs().flush();
        int resIdx = getWindowResolutionIndex();
        boolean vsync = isVsyncEnabled();
        Gdx.app.postRunnable(() -> applyDisplaySettings(mode, resIdx, vsync));
    }

    public void cycleDisplayMode(int dir) {
        DisplayModeOption[] values = DisplayModeOption.values();
        int next = (getDisplayMode().ordinal() + dir + values.length) % values.length;
        setDisplayMode(values[next]);
    }

    public int getWindowResolutionIndex() {
        int idx = prefs().getInteger(PREF_RES_INDEX, 1);
        return Math.max(0, Math.min(idx, WINDOW_RESOLUTIONS.length - 1));
    }

    public void cycleWindowResolution(int dir) {
        int next = (getWindowResolutionIndex() + dir + WINDOW_RESOLUTIONS.length) % WINDOW_RESOLUTIONS.length;
        prefs().putInteger(PREF_RES_INDEX, next);
        prefs().flush();
        if (getDisplayMode() == DisplayModeOption.WINDOWED) {
            boolean vsync = isVsyncEnabled();
            Gdx.app.postRunnable(() -> applyDisplaySettings(DisplayModeOption.WINDOWED, next, vsync));
        }
    }

    public boolean isVsyncEnabled() {
        return prefs().getBoolean(PREF_VSYNC, true);
    }

    public void setVsyncEnabled(boolean enabled) {
        prefs().putBoolean(PREF_VSYNC, enabled);
        prefs().flush();
        Gdx.graphics.setVSync(enabled);
    }

    public void toggleVsync() {
        setVsyncEnabled(!isVsyncEnabled());
    }

    public void toggleFullscreen() {
        changeDisplayModeWithLoading(getDisplayMode() == DisplayModeOption.WINDOWED
                ? DisplayModeOption.FULLSCREEN
                : DisplayModeOption.WINDOWED, getScreen());
    }

    public void changeDisplayModeWithLoading(DisplayModeOption mode, Screen returnScreen) {
        if (mode == getDisplayMode()) return;
        if (getScreen() instanceof DisplayLoadingScreen) return;
        prefs().putString(PREF_DISPLAY_MODE, mode.name());
        prefs().flush();
        int resIdx = getWindowResolutionIndex();
        boolean vsync = isVsyncEnabled();
        setScreen(new DisplayLoadingScreen(this, returnScreen != null ? returnScreen : getScreen(),
                () -> applyDisplaySettings(mode, resIdx, vsync)), false);
    }

    public void changeWindowResolutionWithLoading(int next, Screen returnScreen) {
        if (next == getWindowResolutionIndex()) return;
        if (getScreen() instanceof DisplayLoadingScreen) return;
        prefs().putInteger(PREF_RES_INDEX, next);
        prefs().flush();
        if (getDisplayMode() == DisplayModeOption.WINDOWED) {
            boolean vsync = isVsyncEnabled();
            setScreen(new DisplayLoadingScreen(this, returnScreen != null ? returnScreen : getScreen(),
                    () -> applyDisplaySettings(DisplayModeOption.WINDOWED, next, vsync)), false);
        }
    }

    private void applyDisplaySettings(DisplayModeOption mode, int resIndex, boolean vsync) {
        Gdx.graphics.setVSync(vsync);
        if (mode == DisplayModeOption.FULLSCREEN) {
            Graphics.DisplayMode dm = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setUndecorated(true);
            Gdx.graphics.setWindowedMode(dm.width, dm.height);
        } else {
            int[] res = WINDOW_RESOLUTIONS[resIndex];
            Gdx.graphics.setUndecorated(false);
            Gdx.graphics.setWindowedMode(res[0], res[1]);
        }
    }

    @Override
    public void create() {
        DisplayModeOption mode = getDisplayMode();
        applyDisplaySettings(mode, getWindowResolutionIndex(), isVsyncEnabled());
        if (!prefs().contains(PREF_LANG)) {
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
    public void render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            toggleFullscreen();
        }
        AudioManager.getInstance().update(Gdx.graphics.getDeltaTime());
        stepWikiPreload();
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (ui != null) {
            ui.dispose();
            ui = null;
        }
        if (wikiAssets != null) {
            wikiAssets.dispose();
            wikiAssets = null;
        }
        for (BitmapFont f : wikiFonts.values()) {
            f.dispose();
        }
        wikiFonts.clear();
        AudioManager.getInstance().dispose();
    }
}
