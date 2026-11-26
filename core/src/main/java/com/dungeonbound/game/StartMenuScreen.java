package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.ArrayList;
import java.util.List;

/** Start / Main Menu: Login -> StartMenu -> (Bestiary | Play | Settings | Logout) */
public class StartMenuScreen implements Screen {

    private static final float VW = 1280f;
    private static final float VH = 720f;
    private static final float BTN_W = 420f;
    private static final float BTN_H = 58f;
    private static final float LOGO_W = 554f;
    private static final float LOGO_H = 251f;

    private final Game game;
    private final Stage stage = new Stage(new FitViewport(VW, VH));
    private final List<Texture> textures = new ArrayList<>();
    private final Skin skin = new Skin();

    public StartMenuScreen(Game game) {
        this.game = game;

        // ---- assets (galing sa assets/ui at assets/fonts)
        Texture white = load("ui/white.png", Texture.TextureFilter.Nearest);
        Texture bgTex = load("ui/bg_menu.png", Texture.TextureFilter.Linear);
        Texture logoTex = load("ui/logo_title.png", Texture.TextureFilter.Linear);
        skin.add("white", white, Texture.class);

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/font_body.ttf"));
        BitmapFont fontBody = makeFont(gen, 24);
        BitmapFont fontSmall = makeFont(gen, 18);
        gen.dispose();
        skin.add("font_body", fontBody, BitmapFont.class);
        skin.add("font_small", fontSmall, BitmapFont.class);

        // ---- button styles (itim na button, gold kapag hover)
        TextButton.TextButtonStyle menuStyle = new TextButton.TextButtonStyle();
        menuStyle.font = fontBody;
        menuStyle.fontColor = Color.WHITE;
        menuStyle.overFontColor = new Color(0.95f, 0.69f, 0.20f, 1f);
        menuStyle.downFontColor = new Color(0.70f, 0.50f, 0.12f, 1f);
        menuStyle.up = skin.newDrawable("white", new Color(0f, 0f, 0f, 0.85f));
        menuStyle.over = skin.newDrawable("white", new Color(0.16f, 0.16f, 0.16f, 0.95f));
        menuStyle.down = skin.newDrawable("white", new Color(0.28f, 0.28f, 0.28f, 1f));

        // ---- background
        Image bg = new Image(bgTex);
        bg.setScaling(Scaling.fill);
        bg.setFillParent(true);
        stage.addActor(bg);

        // ---- panel na madilim sa kaliwa (kapareho ng login)
        Image shade = new Image(skin.newDrawable("white", new Color(0f, 0f, 0f, 0.46f)));
        shade.setSize(640f, VH);
        stage.addActor(shade);

        // ---- logo
        Image logo = new Image(logoTex);
        logo.setScaling(Scaling.fit);

        // ---- buttons
        TextButton btnStart = new TextButton(Strings.menuPlay(), menuStyle);
        TextButton btnBestiary = new TextButton(Strings.menuBestiary(), menuStyle);
        TextButton btnSettings = new TextButton(Strings.menuSettings(), menuStyle);
        TextButton btnExit = new TextButton(Strings.menuExit(), menuStyle);

        btnStart.addListener(onChange(() -> {
            AudioManager.getInstance().playBtnSelect();
            this.game.setScreen(new LoadingScreen(this.game));
        }));
        btnBestiary.addListener(onChange(() -> {
            AudioManager.getInstance().playBtnSelect();
            this.game.setScreen(new WikiMenuScreen(this.game));
        }));
        btnSettings.addListener(onChange(() -> {
            AudioManager.getInstance().playBtnSelect();
            SettingsScreen.open((DungeonBound) this.game, () -> {
                AudioManager.getInstance().playBtnDeselect();
                this.game.setScreen(new StartMenuScreen(this.game));
            });
        }));
        btnExit.addListener(onChange(() -> {
            AudioManager.getInstance().playBtnSelect();
            Gdx.app.exit();
        }));

        btnStart.addListener(onHover(() -> AudioManager.getInstance().playBtnHover()));
        btnBestiary.addListener(onHover(() -> AudioManager.getInstance().playBtnHover()));
        btnSettings.addListener(onHover(() -> AudioManager.getInstance().playBtnHover()));
        btnExit.addListener(onHover(() -> AudioManager.getInstance().playBtnHover()));

        Table menu = new Table();
        menu.top();
        menu.add(logo).size(LOGO_W, LOGO_H).padTop(30f).row();
        menu.add(btnStart).size(BTN_W, BTN_H).padTop(30f).row();
        menu.add(btnBestiary).size(BTN_W, BTN_H).padTop(16f).row();
        menu.add(btnSettings).size(BTN_W, BTN_H).padTop(16f).row();
        menu.add(btnExit).size(BTN_W, BTN_H).padTop(16f).row();

        Table root = new Table();
        root.setFillParent(true);
        root.left();
        root.add(menu).width(640f).growY().top();
        root.add().grow();
        stage.addActor(root);
    }

    private Texture load(String path, Texture.TextureFilter filter) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(filter, filter);
        textures.add(t);
        return t;
    }

    private static BitmapFont makeFont(FreeTypeFontGenerator gen, int size) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = size;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
        return gen.generateFont(p);
    }

    private static ChangeListener onChange(final Runnable r) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                r.run();
            }
        };
    }

    private static ClickListener onHover(final Runnable r) {
        return new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) { // hover mouse cursor, hindi touch drag
                    r.run();
                }
            }
        };
    }

    // ---------------------------------------------------------------- Screen

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.5f));

        if (!AudioManager.getInstance().isMusicPlaying()) {
            AudioManager.getInstance().playTitleMusic();
        }
        if (game instanceof DungeonBound) {
            ((DungeonBound) game).startWikiPreload();
        }
    }

    @Override
    public void render(float delta) {
        AudioManager.getInstance().update(delta);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(delta, 1f / 30f));
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        for (Texture t : textures) t.dispose();
    }
}