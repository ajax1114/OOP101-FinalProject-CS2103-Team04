package com.dungeonbound.game;

import com.badlogic.gdx.Application.ApplicationType;
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
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;
import java.util.List;


public class LoginScreen implements Screen {

    private static final float VW = 1280f;
    private static final float VH = 720f;

    private static final float PANEL_W = 640f;
    private static final float FORM_W = 474f;
    private static final float ROW_H = 50f;
    private static final float ICON_W = 51f;
    private static final float TOGGLE_W = 50f;
    private static final float LOGO_W = 554f;
    private static final float LOGO_H = 251f;

    private static final String[] UI_IMAGES = {
        "white", "img_background", "img_logo", "img_iconUsername",
        "img_iconPassword", "img_eyeOpen", "img_eyeClosed"
    };

    private final Game game;
    private final Stage stage;
    private final List<Texture> textures = new ArrayList<>();
    private final Skin skin;

    private Image img_background;
    private Image img_logo;
    private Image img_iconUsername;
    private Image img_iconPassword;
    private Stack bg_iconUsername;
    private Stack bg_iconPassword;
    private TextField input_username;
    private TextField input_password;
    private ImageButton btn_togglePassword;
    private CheckBox chk_rememberMe;
    private Label lbl_error;
    private TextButton link_forgotPassword;
    private TextButton btn_login;
    private TextButton btn_signUp;
    private Table grp_header;
    private Table grp_form;
    private Table grp_actions;

    private boolean loading = false;

    public LoginScreen(Game game) {
        this.game = game;
        this.stage = new Stage(new FitViewport(VW, VH));
        this.skin = createSkin();
        buildUi();
        bindListeners();
        updateLoginEnabled();
    }

    // ------------------------------------------------------------------ skin

    private Skin createSkin() {
        Skin s = new Skin();
        for (String n : UI_IMAGES) {
            Texture t = new Texture(Gdx.files.internal("ui/" + n + ".png"));
            boolean big = n.equals("img_background") || n.equals("img_logo");
            Texture.TextureFilter f = big ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest;
            t.setFilter(f, f);
            textures.add(t);
            s.add(n, t, Texture.class);
        }
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/font_body.ttf"));
        s.add("font_body", makeFont(gen, 24), BitmapFont.class);
        s.add("font_small", makeFont(gen, 18), BitmapFont.class);
        gen.dispose();
        s.load(Gdx.files.internal("ui/uiskin.json"));
        return s;
    }

    private static BitmapFont makeFont(FreeTypeFontGenerator gen, int size) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = size;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
        return gen.generateFont(p);
    }

    private Drawable flat(Color c, float left, float right) {
        Drawable d = skin.newDrawable("white", c);
        if (d instanceof BaseDrawable) {
            ((BaseDrawable) d).setLeftWidth(left);
            ((BaseDrawable) d).setRightWidth(right);
        }
        return d;
    }

    // -------------------------------------------------------------------- UI

    private void buildUi() {
        img_background = new Image(skin.getDrawable("img_background"));
        img_background.setName("img_background");
        img_background.setScaling(Scaling.fill);
        img_background.setFillParent(true);
        stage.addActor(img_background);

        img_logo = new Image(skin.getDrawable("img_logo"));
        img_logo.setName("img_logo");
        img_logo.setScaling(Scaling.fit);
        grp_header = new Table();
        grp_header.setName("grp_header");
        grp_header.add(img_logo).size(LOGO_W, LOGO_H);

        TextField.TextFieldStyle fieldStyle =
                new TextField.TextFieldStyle(skin.get("input", TextField.TextFieldStyle.class));
        fieldStyle.background = flat(new Color(0xd9d9d9ff), 16f, 16f);
        Drawable cursor = skin.newDrawable("white", Color.BLACK);
        if (cursor instanceof BaseDrawable) ((BaseDrawable) cursor).setMinWidth(2f);
        fieldStyle.cursor = cursor;

        img_iconUsername = new Image(skin.getDrawable("img_iconUsername"));
        img_iconUsername.setName("img_iconUsername");
        img_iconPassword = new Image(skin.getDrawable("img_iconPassword"));
        img_iconPassword.setName("img_iconPassword");
        bg_iconUsername = iconBox("bg_iconUsername", img_iconUsername);
        bg_iconPassword = iconBox("bg_iconPassword", img_iconPassword);

        input_username = new TextField("", fieldStyle);
        input_username.setName("input_username");
        input_username.setMessageText(Strings.usernamePlaceholder());

        input_password = new TextField("", fieldStyle);
        input_password.setName("input_password");
        input_password.setMessageText(Strings.passwordPlaceholder());
        input_password.setPasswordCharacter('*');
        input_password.setPasswordMode(true);

        btn_togglePassword = new ImageButton(skin, "btn_togglePassword");
        btn_togglePassword.setName("btn_togglePassword");
        btn_togglePassword.getImage().setScaling(Scaling.fit);
        btn_togglePassword.getImageCell().size(28f);

        lbl_error = new Label("", skin, "error");
        lbl_error.setName("lbl_error");
        lbl_error.setWrap(true);
        lbl_error.setVisible(false);

        chk_rememberMe = new CheckBox(Strings.rememberMe(), skin, "chk_rememberMe");
        chk_rememberMe.setName("chk_rememberMe");
        chk_rememberMe.getImage().setScaling(Scaling.fill);
        chk_rememberMe.getImageCell().size(36f).padRight(14f);

        link_forgotPassword = new TextButton(Strings.forgotPassword(), skin, "link");
        link_forgotPassword.setName("link_forgotPassword");

        btn_login = new TextButton(Strings.loginBtn(), skin, "btn_login");
        btn_login.setName("btn_login");
        btn_signUp = new TextButton(Strings.signUpBtn(), skin, "btn_signUp");
        btn_signUp.setName("btn_signUp");
        grp_actions = new Table();
        grp_actions.setName("grp_actions");
        grp_actions.add(btn_login).size(FORM_W / 2f, ROW_H);
        grp_actions.add(btn_signUp).size(FORM_W / 2f, ROW_H);

        grp_form = new Table();
        grp_form.setName("grp_form");
        grp_form.add(bg_iconUsername).size(ICON_W, ROW_H);
        grp_form.add(input_username).colspan(2).width(FORM_W - ICON_W).height(ROW_H);
        grp_form.row().padTop(18f);
        grp_form.add(bg_iconPassword).size(ICON_W, ROW_H);
        grp_form.add(input_password).width(FORM_W - ICON_W - TOGGLE_W).height(ROW_H);
        grp_form.add(btn_togglePassword).size(TOGGLE_W, ROW_H);
        grp_form.row();
        grp_form.add(lbl_error).colspan(3).width(FORM_W).minHeight(28f).left().padTop(10f);
        grp_form.row();
        grp_form.add(chk_rememberMe).colspan(3).left().height(40f).padTop(8f);
        grp_form.row();
        grp_form.add(link_forgotPassword).colspan(3).right().padTop(6f);
        grp_form.row();
        grp_form.add(grp_actions).colspan(3).padTop(40f);

        Table panel = new Table();
        panel.setName("bg_panelShade");
        panel.setBackground(skin.getDrawable("bg_panelShade"));
        panel.top();
        panel.add(grp_header).row();
        panel.add(grp_form).padTop(40f);

        Table root = new Table();
        root.setName("screen_login");
        root.setFillParent(true);
        root.left();
        root.add(panel).width(PANEL_W).growY();
        root.add().grow();
        stage.addActor(root);
    }

    private Stack iconBox(String name, Image icon) {
        Image bg = new Image(skin.getDrawable("bg_icon"));
        Container<Image> iconHolder = new Container<Image>(icon).size(34f).center();
        Stack box = new Stack();
        box.setName(name);
        box.add(bg);
        box.add(iconHolder);
        return box;
    }

    // ------------------------------------------------------------- listeners

    private void bindListeners() {
        ChangeListener onType = onChange(() -> {
            hideError();
            updateLoginEnabled();
        });
        input_username.addListener(onType);
        input_password.addListener(onType);

        input_username.setFocusTraversal(true);
        input_password.setFocusTraversal(true);

        input_password.setTextFieldListener((tf, c) -> {
            if (c == '\r' || c == '\n') trySubmit();
        });

        btn_togglePassword.addListener(onChange(() ->
                input_password.setPasswordMode(!btn_togglePassword.isChecked())));

        btn_login.addListener(onChange(this::trySubmit));
        btn_signUp.addListener(onChange(this::onSignUpClicked));
        link_forgotPassword.addListener(onChange(this::onForgotPasswordClicked));

        FocusListener keyboard = new FocusListener() {
            @Override
            public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                if (!isMobile()) return;
                if (focused) {
                    Gdx.input.setOnscreenKeyboardVisible(true);
                } else {
                    Gdx.app.postRunnable(() -> {
                        if (!(stage.getKeyboardFocus() instanceof TextField)) {
                            Gdx.input.setOnscreenKeyboardVisible(false);
                        }
                    });
                }
            }
        };
        input_username.addListener(keyboard);
        input_password.addListener(keyboard);

        stage.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!(event.getTarget() instanceof TextField)) stage.setKeyboardFocus(null);
                return false;
            }
        });
    }

    private static ChangeListener onChange(final Runnable r) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                r.run();
            }
        };
    }

    private static boolean isMobile() {
        ApplicationType t = Gdx.app.getType();
        return t == ApplicationType.Android || t == ApplicationType.iOS;
    }

    // ----------------------------------------------------------------- logic

    private void updateLoginEnabled() {
        boolean empty = input_username.getText().trim().isEmpty() || input_password.getText().isEmpty();
        btn_login.setDisabled(loading || empty);
    }

    private void trySubmit() {
        if (loading || btn_login.isDisabled()) return;
        hideError();
        setLoading(true);
        stage.setKeyboardFocus(null);
        if (isMobile()) Gdx.input.setOnscreenKeyboardVisible(false);
        onLoginSubmit();
    }

    // Sets button text, centered, and shrinks the font if it's wider than the button.
    private static void setButtonText(TextButton b, String text, float buttonWidth) {
        Label l = b.getLabel();
        l.setFontScale(1f);
        b.setText(text);
        l.setAlignment(Align.center);
        float max = buttonWidth - 24f;
        float w = l.getPrefWidth();
        if (w > max) l.setFontScale(max / w);
    }

    private void setLoading(boolean value) {
        loading = value;
        setButtonText(btn_login, value ? Strings.loggingIn() : Strings.loginBtn(), FORM_W / 2f);
        input_username.setDisabled(value);
        input_password.setDisabled(value);
        btn_togglePassword.setDisabled(value);
        btn_signUp.setDisabled(value);
        link_forgotPassword.setDisabled(value);
        updateLoginEnabled();
    }

    private void showError(String message) {
        lbl_error.setText(message);
        lbl_error.setVisible(true);
        lbl_error.clearActions();
        lbl_error.getColor().a = 0f;
        lbl_error.addAction(Actions.fadeIn(0.2f));
    }

    private void hideError() {
        lbl_error.clearActions();
        lbl_error.setVisible(false);
    }

    /** PLACEHOLDER: walang tunay na backend. Password na mas maikli sa 4 = fail. */
    private void onLoginSubmit() {
        final String username = input_username.getText().trim();
        final String password = input_password.getText();
        final boolean remember = chk_rememberMe.isChecked();
        Gdx.app.log("LoginScreen", "onLoginSubmit user=" + username + " remember=" + remember);

        stage.addAction(Actions.sequence(
                Actions.delay(1.5f),
                Actions.run(() -> {
                    if (password.length() < 4) {
                        onLoginFailed(Strings.errInvalidLogin());
                    } else {
                        onLoginSuccess();
                    }
                })));
    }

    private void onLoginSuccess() {
    setLoading(false);
    game.setScreen(new StartMenuScreen(game));
}

    private void onLoginFailed(String message) {
        setLoading(false);
        showError(message);
    }

    private void onForgotPasswordClicked() {
        Gdx.app.log("LoginScreen", "TODO: open Forgot Password screen");
    }

private void onSignUpClicked() {
    game.setScreen(new SignUpScreen(game));
}

    // ---------------------------------------------------------------- Screen

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.6f));
        if (!isMobile()) stage.setKeyboardFocus(input_username);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(delta, 1f / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() { }

    @Override public void resume() { }

    @Override
    public void hide() {
        if (isMobile()) Gdx.input.setOnscreenKeyboardVisible(false);
        if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        for (Texture t : textures) t.dispose();
    }
}