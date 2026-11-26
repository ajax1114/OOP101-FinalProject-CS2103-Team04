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

    // Actors
    private Image imgBackground;
    private Image imgLogo;
    private Label lblUsername;
    private Label lblPassword;
    private Label lblConfirmPassword;
    private TextField inputUsername;
    private TextField inputPassword;
    private TextField inputConfirmPassword;
    private Label lblError;
    private TextButton btnLogin;
    private TextButton btnSignUp;
    private Table grpHeader;
    private Table grpForm;
    private Table grpActions;

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
        imgBackground = new Image(image("ui/bg_menu.png"));
        imgBackground.setName("img_background");
        imgBackground.setScaling(Scaling.fill);
        imgBackground.setFillParent(true);
        stage.addActor(imgBackground);

        // --- grp_header
        imgLogo = new Image(image("ui/logo_title.png"));
        imgLogo.setName("img_logo");
        imgLogo.setScaling(Scaling.fit);
        grpHeader = new Table();
        grpHeader.setName("grp_header");
        grpHeader.add(imgLogo).size(LOGO_W, LOGO_H);

        // --- grp_form
        lblUsername = new Label(Strings.lblUsername(), labelStyle);
        lblUsername.setName("lbl_username");
        lblPassword = new Label(Strings.lblPassword(), labelStyle);
        lblPassword.setName("lbl_password");
        lblConfirmPassword = new Label(Strings.lblConfirmPassword(), labelStyle);
        lblConfirmPassword.setName("lbl_confirmPassword");

        inputUsername = new TextField("", fieldStyle);
        inputUsername.setName("input_username");

        inputPassword = new TextField("", fieldStyle);
        inputPassword.setName("input_password");
        inputPassword.setPasswordCharacter('*');
        inputPassword.setPasswordMode(true);

        inputConfirmPassword = new TextField("", fieldStyle);
        inputConfirmPassword.setName("input_confirmPassword");
        inputConfirmPassword.setPasswordCharacter('*');
        inputConfirmPassword.setPasswordMode(true);

        lblError = new Label("", errorStyle);
        lblError.setName("lbl_error");
        lblError.setWrap(true);
        lblError.setVisible(false);

        // --- grp_actions
        btnLogin = new TextButton(Strings.loginBtn(), loginStyle);
        btnLogin.setName("btn_login");
        btnSignUp = new TextButton(Strings.signUpBtn(), signUpStyle);
        btnSignUp.setName("btn_signUp");
        grpActions = new Table();
        grpActions.setName("grp_actions");
        grpActions.add(btnLogin).size(FORM_W / 2f, ROW_H);
        grpActions.add(btnSignUp).size(FORM_W / 2f, ROW_H);

        grpForm = new Table();
        grpForm.setName("grp_form");
        grpForm.add(lblUsername).left().padBottom(6f).row();
        grpForm.add(inputUsername).width(FORM_W).height(ROW_H).row();
        grpForm.add(lblPassword).left().padTop(14f).padBottom(6f).row();
        grpForm.add(inputPassword).width(FORM_W).height(ROW_H).row();
        grpForm.add(lblConfirmPassword).left().padTop(14f).padBottom(6f).row();
        grpForm.add(inputConfirmPassword).width(FORM_W).height(ROW_H).row();
        grpForm.add(lblError).width(FORM_W).minHeight(28f).left().padTop(8f).row();
        grpForm.add(grpActions).padTop(16f);

        // --- left panel (shade) + root
        Table panel = new Table();
        panel.setName("bg_panelShade");
        panel.setBackground(tint(0f, 0f, 0f, 0.46f));
        panel.top();
        panel.add(grpHeader).row();
        panel.add(grpForm).padTop(20f);

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
        inputUsername.addListener(onType);
        inputPassword.addListener(onType);
        inputConfirmPassword.addListener(onType);

        // Tab: username -> password -> confirm (default focus traversal, nasa iisang Table)
        inputUsername.setFocusTraversal(true);
        inputPassword.setFocusTraversal(true);
        inputConfirmPassword.setFocusTraversal(true);

        // Enter sa huling field = pindutin ang SIGN UP
        inputConfirmPassword.setTextFieldListener((tf, c) -> {
            if (c == '\r' || c == '\n') trySubmit();
        });

        btnSignUp.addListener(onChange(this::trySubmit));
        btnLogin.addListener(onChange(this::onLoginClicked));
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
        boolean empty = inputUsername.getText().trim().isEmpty()
                || inputPassword.getText().isEmpty()
                || inputConfirmPassword.getText().isEmpty();
        btnSignUp.setDisabled(loading || empty);
    }

    private void trySubmit() {
        if (loading || btnSignUp.isDisabled()) return;
        AudioManager.getInstance().playBtnSelect();

        String pw = inputPassword.getText();
        if (pw.length() < MIN_PASSWORD_LENGTH) {
            showError("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
            return;
        }
        if (!pw.equals(inputConfirmPassword.getText())) {
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
        setButtonText(btnSignUp, value ? Strings.signingUp() : Strings.signUpBtn(), FORM_W / 2f);
        inputUsername.setDisabled(value);
        inputPassword.setDisabled(value);
        inputConfirmPassword.setDisabled(value);
        btnLogin.setDisabled(value);
        updateSignUpEnabled();
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

    /**
     * PLACEHOLDER: walang totoong backend. Palitan ang laman ng iyong register call,
     * tapos tawagin ang onSignUpSuccess() o onSignUpFailed(message).
     */
    private void onSignUpSubmit() {
        Gdx.app.log("SignUpScreen", "onSignUpSubmit user=" + inputUsername.getText().trim());
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
        AudioManager.getInstance().playBtnSelect();
        game.setScreen(new LoginScreen(game));
    }

    // ---------------------------------------------------------------- Screen

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.6f));
        stage.setKeyboardFocus(inputUsername);
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
