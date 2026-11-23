package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;
import java.util.List;

/**
 * Sign-up screen (Figma frame: screen_signUp). Variable names == Actor names == Figma layer names.
 * Self-contained: walang uiskin.json na kailangan.
 *
 * Assets: ui/img_background.png, ui/img_logo.png (pareho sa login), fonts/font_body.ttf
 */
public class SignUpScreen implements Screen {

    private static final float VW = 1280f;
    private static final float VH = 720f;

    private static final float PANEL_W = 640f;
    private static final float FORM_W = 474f;
    private static final float ROW_H = 50f;
    private static final float LOGO_W = 554f;
    private static final float LOGO_H = 251f;
    private static final int MIN_PASSWORD_LENGTH = 4;

    private final Game game;
    private final Stage stage = new Stage(new FitViewport(VW, VH));
    private final List<Texture> textures = new ArrayList<>();
    private final List<BitmapFont> fonts = new ArrayList<>();

    private BitmapFont fontBody;
    private BitmapFont fontSmall;
    private TextureRegionDrawable white; // 1 puting pixel; tina-tint para sa lahat ng kulay

    // Actors (same names as Figma layers)
    private Image img_background;
    private Image img_logo;
    private Label lbl_username;
    private Label lbl_password;
    private Label lbl_confirmPassword;
    private TextField input_username;
    private TextField input_password;
    private TextField input_confirmPassword;
    private Label lbl_error;
    private TextButton btn_login;
    private TextButton btn_signUp;
    private Table grp_header;
    private Table grp_form;
    private Table grp_actions;

    private boolean loading = false;

    public SignUpScreen(Game game) {
        this.game = game;
        loadFonts();
        white = pixel();
        buildUi();
        bindListeners();
        updateSignUpEnabled();
    }

    // ---------------------------------------------------------------- assets

    private void loadFonts() {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/font_body.ttf"));
        fontBody = makeFont(gen, 24);
        fontSmall = makeFont(gen, 18);
        gen.dispose();
    }

    private BitmapFont makeFont(FreeTypeFontGenerator gen, int size) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = size;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
        BitmapFont f = gen.generateFont(p);
        fonts.add(f);
        return f;
    }

    private TextureRegionDrawable pixel() {
        Pixmap pm = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        textures.add(t);
        return new TextureRegionDrawable(new TextureRegion(t));
    }

    private Drawable tint(float r, float g, float b, float a) {
        return white.tint(new Color(r, g, b, a));
    }

    private Drawable image(String path) {
        FileHandle fh = Gdx.files.internal(path);
        if (!fh.exists()) {
            Gdx.app.error("SignUpScreen", "Missing asset: " + path);
            return new BaseDrawable();
        }
        Texture t = new Texture(fh);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(t);
        return new TextureRegionDrawable(new TextureRegion(t));
    }

    // -------------------------------------------------------------------- UI

    private void buildUi() {
        // --- styles
        Label.LabelStyle labelStyle = new Label.LabelStyle(fontBody, Color.WHITE);
        Label.LabelStyle errorStyle = new Label.LabelStyle(fontSmall, new Color(1f, 0.42f, 0.42f, 1f));

        TextField.TextFieldStyle fieldStyle = new TextField.TextFieldStyle();
        fieldStyle.font = fontBody;
        fieldStyle.fontColor = Color.BLACK;
        fieldStyle.disabledFontColor = new Color(0.4f, 0.4f, 0.4f, 1f);
        fieldStyle.messageFont = fontBody;
        fieldStyle.messageFontColor = new Color(0.4f, 0.4f, 0.4f, 1f);
        Drawable fieldBg = tint(0.85f, 0.85f, 0.85f, 1f);
        if (fieldBg instanceof BaseDrawable) {
            ((BaseDrawable) fieldBg).setLeftWidth(16f);
            ((BaseDrawable) fieldBg).setRightWidth(16f);
        }
        fieldStyle.background = fieldBg;
        Drawable cursor = tint(0f, 0f, 0f, 1f);
        if (cursor instanceof BaseDrawable) ((BaseDrawable) cursor).setMinWidth(2f);
        fieldStyle.cursor = cursor;
        fieldStyle.selection = tint(0.2f, 0.5f, 1f, 0.4f);

        // Kaliwang button (LOGIN): light
        TextButton.TextButtonStyle loginStyle = new TextButton.TextButtonStyle();
        loginStyle.font = fontBody;
        loginStyle.fontColor = Color.BLACK;
        loginStyle.disabledFontColor = new Color(0.55f, 0.55f, 0.55f, 1f);
        loginStyle.up = tint(0.85f, 0.85f, 0.85f, 1f);
        loginStyle.over = tint(0.95f, 0.95f, 0.95f, 1f);
        loginStyle.down = tint(0.62f, 0.62f, 0.62f, 1f);
        loginStyle.disabled = tint(0.3f, 0.3f, 0.3f, 1f);

        // Kanang button (SIGN UP): dark
        TextButton.TextButtonStyle signUpStyle = new TextButton.TextButtonStyle();
        signUpStyle.font = fontBody;
        signUpStyle.fontColor = Color.WHITE;
        signUpStyle.disabledFontColor = new Color(0.55f, 0.55f, 0.55f, 1f);
        signUpStyle.up = tint(0f, 0f, 0f, 1f);
        signUpStyle.over = tint(0.16f, 0.16f, 0.16f, 1f);
        signUpStyle.down = tint(0.28f, 0.28f, 0.28f, 1f);
        signUpStyle.disabled = tint(0.1f, 0.1f, 0.1f, 1f);

        // --- grp_background
        img_background = new Image(image("ui/img_background.png"));
        img_background.setName("img_background");
        img_background.setScaling(Scaling.fill);
        img_background.setFillParent(true);
        stage.addActor(img_background);

        // --- grp_header
        img_logo = new Image(image("ui/img_logo.png"));
        img_logo.setName("img_logo");
        img_logo.setScaling(Scaling.fit);
        grp_header = new Table();
        grp_header.setName("grp_header");
        grp_header.add(img_logo).size(LOGO_W, LOGO_H);

        // --- grp_form
        lbl_username = new Label(Strings.lblUsername(), labelStyle);
        lbl_username.setName("lbl_username");
        lbl_password = new Label(Strings.lblPassword(), labelStyle);
        lbl_password.setName("lbl_password");
        lbl_confirmPassword = new Label(Strings.lblConfirmPassword(), labelStyle);
        lbl_confirmPassword.setName("lbl_confirmPassword");

        input_username = new TextField("", fieldStyle);
        input_username.setName("input_username");

        input_password = new TextField("", fieldStyle);
        input_password.setName("input_password");
        input_password.setPasswordCharacter('*');
        input_password.setPasswordMode(true);

        input_confirmPassword = new TextField("", fieldStyle);
        input_confirmPassword.setName("input_confirmPassword");
        input_confirmPassword.setPasswordCharacter('*');
        input_confirmPassword.setPasswordMode(true);

        lbl_error = new Label("", errorStyle);
        lbl_error.setName("lbl_error");
        lbl_error.setWrap(true);
        lbl_error.setVisible(false);

        // --- grp_actions
        btn_login = new TextButton(Strings.loginBtn(), loginStyle);
        btn_login.setName("btn_login");
        btn_signUp = new TextButton(Strings.signUpBtn(), signUpStyle);
        btn_signUp.setName("btn_signUp");
        grp_actions = new Table();
        grp_actions.setName("grp_actions");
        grp_actions.add(btn_login).size(FORM_W / 2f, ROW_H);
        grp_actions.add(btn_signUp).size(FORM_W / 2f, ROW_H);

        grp_form = new Table();
        grp_form.setName("grp_form");
        grp_form.add(lbl_username).left().padBottom(6f).row();
        grp_form.add(input_username).width(FORM_W).height(ROW_H).row();
        grp_form.add(lbl_password).left().padTop(14f).padBottom(6f).row();
        grp_form.add(input_password).width(FORM_W).height(ROW_H).row();
        grp_form.add(lbl_confirmPassword).left().padTop(14f).padBottom(6f).row();
        grp_form.add(input_confirmPassword).width(FORM_W).height(ROW_H).row();
        grp_form.add(lbl_error).width(FORM_W).minHeight(28f).left().padTop(8f).row();
        grp_form.add(grp_actions).padTop(16f);

        // --- left panel (shade) + root
        Table panel = new Table();
        panel.setName("bg_panelShade");
        panel.setBackground(tint(0f, 0f, 0f, 0.46f));
        panel.top();
        panel.add(grp_header).row();
        panel.add(grp_form).padTop(20f);

        Table root = new Table();
        root.setName("screen_signUp");
        root.setFillParent(true);
        root.left();
        root.add(panel).width(PANEL_W).growY();
        root.add().grow();
        stage.addActor(root);
    }

    // ------------------------------------------------------------- listeners

    private void bindListeners() {
        ChangeListener onType = onChange(() -> {
            hideError();
            updateSignUpEnabled();
        });
        input_username.addListener(onType);
        input_password.addListener(onType);
        input_confirmPassword.addListener(onType);

        // Tab: username -> password -> confirm (default focus traversal, nasa iisang Table)
        input_username.setFocusTraversal(true);
        input_password.setFocusTraversal(true);
        input_confirmPassword.setFocusTraversal(true);

        // Enter sa huling field = pindutin ang SIGN UP
        input_confirmPassword.setTextFieldListener((tf, c) -> {
            if (c == '\r' || c == '\n') trySubmit();
        });

        btn_signUp.addListener(onChange(this::trySubmit));
        btn_login.addListener(onChange(this::onLoginClicked));
    }

    private static ChangeListener onChange(final Runnable r) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                r.run();
            }
        };
    }

    // ----------------------------------------------------------------- logic

    private void updateSignUpEnabled() {
        boolean empty = input_username.getText().trim().isEmpty()
                || input_password.getText().isEmpty()
                || input_confirmPassword.getText().isEmpty();
        btn_signUp.setDisabled(loading || empty);
    }

    private void trySubmit() {
        if (loading || btn_signUp.isDisabled()) return;

        String pw = input_password.getText();
        if (pw.length() < MIN_PASSWORD_LENGTH) {
            showError("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
            return;
        }
        if (!pw.equals(input_confirmPassword.getText())) {
            showError(Strings.errPasswordsNotMatch());
            return;
        }

        hideError();
        setLoading(true);
        stage.setKeyboardFocus(null);
        onSignUpSubmit();
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
        setButtonText(btn_signUp, value ? Strings.signingUp() : Strings.signUpBtn(), FORM_W / 2f);
        input_username.setDisabled(value);
        input_password.setDisabled(value);
        input_confirmPassword.setDisabled(value);
        btn_login.setDisabled(value);
        updateSignUpEnabled();
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

    /**
     * PLACEHOLDER: walang totoong backend. Palitan ang laman ng iyong register call,
     * tapos tawagin ang onSignUpSuccess() o onSignUpFailed(message).
     */
    private void onSignUpSubmit() {
        Gdx.app.log("SignUpScreen", "onSignUpSubmit user=" + input_username.getText().trim());
        stage.addAction(Actions.sequence(
                Actions.delay(1.5f), // fake network wait
                Actions.run(this::onSignUpSuccess)));
    }

    private void onSignUpSuccess() {
        setLoading(false);
        Gdx.app.log("SignUpScreen", "TODO: account created -> balik sa LoginScreen");
        game.setScreen(new LoginScreen(game));
    }

    @SuppressWarnings("unused") // gagamitin kapag may backend na
    private void onSignUpFailed(String message) {
        setLoading(false);
        showError(message);
    }

    private void onLoginClicked() {
        game.setScreen(new LoginScreen(game));
    }

    // ---------------------------------------------------------------- Screen

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.6f));
        stage.setKeyboardFocus(input_username);
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
        if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        for (Texture t : textures) t.dispose();
        for (BitmapFont f : fonts) f.dispose();
    }
}
