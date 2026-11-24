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
        "white", "bg_menu", "logo_title", "icon_username",
        "icon_password", "icon_eye_open", "icon_eye_closed"
    };

    private final Game game;
    private final Stage stage;
    private final List<Texture> textures = new ArrayList<>();
    private final Skin skin;

    private Image imgBackground;
    private Image imgLogo;
    private Image imgIconUsername;
    private Image imgIconPassword;
    private Stack bgIconUsername;
    private Stack bgIconPassword;
    private TextField inputUsername;
    private TextField inputPassword;
    private ImageButton btnTogglePassword;
    private CheckBox chkRememberMe;
    private Label lblError;
    private TextButton linkForgotPassword;
    private TextButton btnLogin;
    private TextButton btnSignUp;
    private Table grpHeader;
    private Table grpForm;
    private Table grpActions;

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
            boolean big = n.equals("bg_menu") || n.equals("logo_title");
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
        imgBackground = new Image(skin.getDrawable("bg_menu"));
        imgBackground.setName("img_background");
        imgBackground.setScaling(Scaling.fill);
        imgBackground.setFillParent(true);
        stage.addActor(imgBackground);

        imgLogo = new Image(skin.getDrawable("logo_title"));
        imgLogo.setName("img_logo");
        imgLogo.setScaling(Scaling.fit);
        grpHeader = new Table();
        grpHeader.setName("grp_header");
        grpHeader.add(imgLogo).size(LOGO_W, LOGO_H);

        TextField.TextFieldStyle fieldStyle =
                new TextField.TextFieldStyle(skin.get("input", TextField.TextFieldStyle.class));
        fieldStyle.background = flat(new Color(0xd9d9d9ff), 16f, 16f);
        Drawable cursor = skin.newDrawable("white", Color.BLACK);
        if (cursor instanceof BaseDrawable) ((BaseDrawable) cursor).setMinWidth(2f);
        fieldStyle.cursor = cursor;

        imgIconUsername = new Image(skin.getDrawable("icon_username"));
        imgIconUsername.setName("img_iconUsername");
        imgIconPassword = new Image(skin.getDrawable("icon_password"));
        imgIconPassword.setName("img_iconPassword");
        bgIconUsername = iconBox("bg_iconUsername", imgIconUsername);
        bgIconPassword = iconBox("bg_iconPassword", imgIconPassword);

        inputUsername = new TextField("", fieldStyle);
        inputUsername.setName("input_username");
        inputUsername.setMessageText(Strings.usernamePlaceholder());

        inputPassword = new TextField("", fieldStyle);
        inputPassword.setName("input_password");
        inputPassword.setMessageText(Strings.passwordPlaceholder());
        inputPassword.setPasswordCharacter('*');
        inputPassword.setPasswordMode(true);

        btnTogglePassword = new ImageButton(skin, "btn_togglePassword");
        btnTogglePassword.setName("btn_togglePassword");
        btnTogglePassword.getImage().setScaling(Scaling.fit);
        btnTogglePassword.getImageCell().size(28f);

        lblError = new Label("", skin, "error");
        lblError.setName("lbl_error");
        lblError.setWrap(true);
        lblError.setVisible(false);

        chkRememberMe = new CheckBox(Strings.rememberMe(), skin, "chk_rememberMe");
        chkRememberMe.setName("chk_rememberMe");
        chkRememberMe.getImage().setScaling(Scaling.fill);
        chkRememberMe.getImageCell().size(36f).padRight(14f);

        linkForgotPassword = new TextButton(Strings.forgotPassword(), skin, "link");
        linkForgotPassword.setName("link_forgotPassword");

        btnLogin = new TextButton(Strings.loginBtn(), skin, "btn_login");
        btnLogin.setName("btn_login");
        btnSignUp = new TextButton(Strings.signUpBtn(), skin, "btn_signUp");
        btnSignUp.setName("btn_signUp");
        grpActions = new Table();
        grpActions.setName("grp_actions");
        grpActions.add(btnLogin).size(FORM_W / 2f, ROW_H);
        grpActions.add(btnSignUp).size(FORM_W / 2f, ROW_H);

        grpForm = new Table();
        grpForm.setName("grp_form");
        grpForm.add(bgIconUsername).size(ICON_W, ROW_H);
        grpForm.add(inputUsername).colspan(2).width(FORM_W - ICON_W).height(ROW_H);
        grpForm.row().padTop(18f);
        grpForm.add(bgIconPassword).size(ICON_W, ROW_H);
        grpForm.add(inputPassword).width(FORM_W - ICON_W - TOGGLE_W).height(ROW_H);
        grpForm.add(btnTogglePassword).size(TOGGLE_W, ROW_H);
        grpForm.row();
        grpForm.add(lblError).colspan(3).width(FORM_W).minHeight(28f).left().padTop(10f);
        grpForm.row();
        grpForm.add(chkRememberMe).colspan(3).left().height(40f).padTop(8f);
        grpForm.row();
        grpForm.add(linkForgotPassword).colspan(3).right().padTop(6f);
        grpForm.row();
        grpForm.add(grpActions).colspan(3).padTop(40f);

        Table panel = new Table();
        panel.setName("bg_panelShade");
        panel.setBackground(skin.getDrawable("bg_panelShade"));
        panel.top();
        panel.add(grpHeader).row();
        panel.add(grpForm).padTop(40f);

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
        inputUsername.addListener(onType);
        inputPassword.addListener(onType);

        inputUsername.setFocusTraversal(true);
        inputPassword.setFocusTraversal(true);

        inputPassword.setTextFieldListener((tf, c) -> {
            if (c == '\r' || c == '\n') trySubmit();
        });

        btnTogglePassword.addListener(onChange(() ->
                inputPassword.setPasswordMode(!btnTogglePassword.isChecked())));

        btnLogin.addListener(onChange(this::trySubmit));
        btnSignUp.addListener(onChange(this::onSignUpClicked));
        linkForgotPassword.addListener(onChange(this::onForgotPasswordClicked));

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
        inputUsername.addListener(keyboard);
        inputPassword.addListener(keyboard);

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
        boolean empty = inputUsername.getText().trim().isEmpty() || inputPassword.getText().isEmpty();
        btnLogin.setDisabled(loading || empty);
    }

    private void trySubmit() {
        if (loading || btnLogin.isDisabled()) return;
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
        setButtonText(btnLogin, value ? Strings.loggingIn() : Strings.loginBtn(), FORM_W / 2f);
        inputUsername.setDisabled(value);
        inputPassword.setDisabled(value);
        btnTogglePassword.setDisabled(value);
        btnSignUp.setDisabled(value);
        linkForgotPassword.setDisabled(value);
        updateLoginEnabled();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.clearActions();
        lblError.getColor().a = 0f;
        lblError.addAction(Actions.fadeIn(0.2f));
    }

    private void hideError() {
        lblError.clearActions();
        lblError.setVisible(false);
    }

    /** PLACEHOLDER: walang tunay na backend. Password na mas maikli sa 4 = fail. */
    private void onLoginSubmit() {
        final String username = inputUsername.getText().trim();
        final String password = inputPassword.getText();
        final boolean remember = chkRememberMe.isChecked();
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
        if (!isMobile()) stage.setKeyboardFocus(inputUsername);
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