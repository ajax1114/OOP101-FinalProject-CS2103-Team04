package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen (640x360, y pababa gaya ng Ui). Apat na tab: Account, Gameplay Layout, Audio, Language.
 *
 * 9-slice:
 *  - Malaking board  -> ui.board(...)  (frame_clean.png, hiwa 8/8/15/15)
 *  - Content panel   -> sariling NinePatch mula sa parehong frame_clean.png
 *
 * Kontrol: W/S o ↑/↓ = lipat, D/→ = pasok sa content / dagdag, A/← = labas / bawas,
 *          Enter/Space = piliin, Esc = atras. (Susunod ang GameControls bindings, pero laging gumagana ang arrows/Esc.)
 */
public final class SettingsScreen extends ScreenAdapter {

    /** Koneksyon sa account system. Palitan ng SQLite-backed na implementasyon sa susunod. */
    public interface AccountService {
        /** Pangalan ng naka-login, o null kung bisita. */
        String username();
        void logout();
    }

    /** Isang tawag lang para buksan ang Settings. Palitan ang AccountService kapag handa na ang SQLite. */
    public static void open(final DungeonBound game, Runnable onBack) {
        game.setScreen(new SettingsScreen(game, new AccountService() {
            @Override public String username() { return null; } // TODO: SQLite
            @Override public void logout() { game.setScreen(new LoginScreen(game)); }
        }, onBack));
    }

    // ---- layout (640x360, y pababa)
    private static final int BX = 40, BY = 36, BW = 560, BH = 314;              // malaking board (BY=36 para litaw ang fleur sa taas)
    private static final int TAB_X = 62, TAB_Y = 92, TAB_W = 128, TAB_H = 28, TAB_STEP = 33;
    private static final int BACK_Y = 260;
    private static final int PX = 204, PY = 90, PW = 374, PH = 246;              // content panel
    private static final int ROW_X = PX + 14, ROW_W = PW - 28, ROW_H = 30, ROW_STEP = 35, ROW_Y0 = 134;
    private static final int TRACK_X = ROW_X + 150, TRACK_W = 130;               // slider
    private static final int TAB_COUNT = 4, LEFT_COUNT = 5;                      // 4 tab + Back

    private static final Color[] SHADOW = {
            Color.valueOf("462104"), Color.valueOf("3f2214"), Color.valueOf("5e4133")
    };

    private final DungeonBound game;
    private final AccountService account;
    private final Runnable onBack;
    private final Ui ui;
    private final PixelViewport view = new PixelViewport();
    private final Texture panelTex;
    private final NinePatch panel;
    private final Vector2 tmp = new Vector2();
    private final List<List<Row>> rows = new ArrayList<>();

    private boolean tl = true;          // kasalukuyang wika (TL = Tagalog)
    private int leftSel = 0;            // 0..3 = tab, 4 = Back
    private int tab = 0;
    private int row = 0;
    private boolean inContent = false;
    private boolean dragging = false;
    private GameControls.Action listening = null;
    private String toast = "";
    private float toastTimer = 0f;

    public SettingsScreen(DungeonBound game, AccountService account, Runnable onBack) {
        this.game = game;
        this.account = account;
        this.onBack = onBack;
        this.ui = game.getUi();

        panelTex = new Texture(Gdx.files.internal("ui/frame_clean.png"));
        panelTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        panel = new NinePatch(panelTex, 8, 8, 15, 15);

        buildRows();
    }

    // ------------------------------------------------------------- rows

    private String text(String tagalog, String english) {
        return tl ? tagalog : english;
    }

    private abstract class Row {
        abstract String label();
        String value() { return ""; }
        float slider() { return -1f; }          // -1 = hindi slider
        boolean checked() { return false; }
        void adjust(int dir) {}
        void setSlider(float v) {}
        void activate() {}
    }

    private final class SimpleRow extends Row {
        private final String t, e;
        private final Runnable action;
        SimpleRow(String t, String e, Runnable action) { this.t = t; this.e = e; this.action = action; }
        @Override String label() { return text(t, e); }
        @Override void activate() { action.run(); }
    }

    private final class VolumeRow extends Row {
        private final int kind; // 0 music, 1 ambience, 2 sfx
        private final String t, e;
        VolumeRow(int kind, String t, String e) { this.kind = kind; this.t = t; this.e = e; }

        private float get() {
            AudioManager a = AudioManager.getInstance();
            return kind == 0 ? a.getMusicVolume() : kind == 1 ? a.getAmbienceVolume() : a.getSfxVolume();
        }
        private void set(float v) {
            AudioManager a = AudioManager.getInstance();
            if (kind == 0) a.setMusicVolume(v);
            else if (kind == 1) a.setAmbienceVolume(v);
            else a.setSfxVolume(v);
        }
        @Override String label() { return text(t, e); }
        @Override String value() { return Math.round(get() * 100f) + "%"; }
        @Override float slider() { return get(); }
        @Override void adjust(int dir) {
            set(MathUtils.clamp(Math.round(get() * 10f) / 10f + dir * 0.1f, 0f, 1f));
            if (kind == 2) AudioManager.getInstance().playSfx("audio/sfx_whoosh.wav", 0.5f);
        }
        @Override void setSlider(float v) { set(MathUtils.clamp(Math.round(v * 20f) / 20f, 0f, 1f)); }
    }

    private final class KeyRow extends Row {
        private final GameControls.Action action;
        private final String t, e;
        KeyRow(GameControls.Action action, String t, String e) { this.action = action; this.t = t; this.e = e; }
        @Override String label() { return text(t, e); }
        @Override String value() {
            if (listening == action) return "...";
            StringBuilder sb = new StringBuilder();
            for (int k : GameControls.get(action)) {
                if (sb.length() > 0) sb.append(" / ");
                sb.append(Input.Keys.toString(k));
            }
            return sb.toString();
        }
        @Override void activate() { listening = action; }
    }

    private final class LangRow extends Row {
        private final Lang lang;
        private final String name;
        LangRow(Lang lang, String name) { this.lang = lang; this.name = name; }
        @Override String label() { return name; }
        @Override boolean checked() { return game.getSelectedLanguage() == lang; }
        @Override void activate() { game.setLanguage(lang); }
    }

    private void buildRows() {
        // 0: Account
        List<Row> acc = new ArrayList<>();
        acc.add(new SimpleRow("Palitan ang Password", "Change Password", () ->
                showToast(text("Susunod: SQLite", "Coming soon: SQLite")))); // TODO: SQLite
        acc.add(new SimpleRow("Mag-logout", "Log out", account::logout));
        rows.add(acc);

        // 1: Gameplay Layout (key bindings, galing sa GameControls)
        List<Row> gp = new ArrayList<>();
        gp.add(new KeyRow(GameControls.Action.LEFT, "Pakaliwa", "Move Left"));
        gp.add(new KeyRow(GameControls.Action.RIGHT, "Pakanan", "Move Right"));
        gp.add(new KeyRow(GameControls.Action.SELECT, "Piliin", "Select"));
        gp.add(new KeyRow(GameControls.Action.BACK, "Bumalik", "Back"));
        gp.add(new SimpleRow("I-reset sa Default", "Reset to Default", () -> {
            for (GameControls.Action a : GameControls.Action.values()) GameControls.set(a, a.defaults);
            showToast(text("Naibalik na ang default", "Defaults restored"));
        }));
        rows.add(gp);

        // 2: Audio
        List<Row> au = new ArrayList<>();
        au.add(new VolumeRow(0, "Musika", "Music"));
        au.add(new VolumeRow(1, "Paligid", "Ambience"));
        au.add(new VolumeRow(2, "Epekto (SFX)", "Effects (SFX)"));
        rows.add(au);

        // 3: Language
        List<Row> lg = new ArrayList<>();
        lg.add(new LangRow(Lang.TL, "Tagalog"));
        lg.add(new LangRow(Lang.EN, "English"));
        rows.add(lg);
    }

    private List<Row> curRows() { return rows.get(tab); }

    private void showToast(String s) {
        toast = s;
        toastTimer = 2f;
    }

    // ------------------------------------------------------------- navigation

    private void move(int d) {
        if (!inContent) {
            leftSel = (leftSel + d + LEFT_COUNT) % LEFT_COUNT;
            if (leftSel < TAB_COUNT) { tab = leftSel; row = 0; }
        } else {
            int n = curRows().size();
            row = (row + d + n) % n;
        }
    }

    private void enterContent() {
        if (leftSel < TAB_COUNT) { inContent = true; row = 0; }
    }

    private void leaveContent() { inContent = false; }

    private static boolean isUp(int k) { return k == Input.Keys.UP || k == Input.Keys.W; }
    private static boolean isDown(int k) { return k == Input.Keys.DOWN || k == Input.Keys.S; }
    private static boolean isLeft(int k) { return k == Input.Keys.LEFT || GameControls.is(GameControls.Action.LEFT, k); }
    private static boolean isRight(int k) { return k == Input.Keys.RIGHT || GameControls.is(GameControls.Action.RIGHT, k); }
    private static boolean isSelect(int k) { return k == Input.Keys.ENTER || GameControls.is(GameControls.Action.SELECT, k); }
    private static boolean isBack(int k) { return k == Input.Keys.ESCAPE || GameControls.is(GameControls.Action.BACK, k); }

    // ------------------------------------------------------------- hit tests (640x360, y pababa)

    private static int leftY(int i) { return i < TAB_COUNT ? TAB_Y + i * TAB_STEP : BACK_Y; }

    private int leftHit(float x, float y) {
        for (int i = 0; i < LEFT_COUNT; i++) {
            int by = leftY(i);
            if (x >= TAB_X && x < TAB_X + TAB_W && y >= by && y < by + TAB_H) return i;
        }
        return -1;
    }

    private int rowHit(float x, float y) {
        List<Row> list = curRows();
        for (int i = 0; i < list.size(); i++) {
            int by = ROW_Y0 + i * ROW_STEP;
            if (x >= ROW_X && x < ROW_X + ROW_W && y >= by && y < by + ROW_H) return i;
        }
        return -1;
    }

    private Vector2 toUi(int sx, int sy) {
        view.unproject(tmp.set(sx, sy));
        tmp.y = PixelViewport.H - tmp.y;
        return tmp;
    }

    private void dragSlider(float x) {
        Row r = curRows().get(row);
        if (r.slider() >= 0f) r.setSlider((x - TRACK_X) / TRACK_W);
    }

    // ------------------------------------------------------------- input

    private final InputAdapter input = new InputAdapter() {
        @Override
        public boolean keyDown(int k) {
            if (listening != null) {
                if (k != Input.Keys.ESCAPE) {
                    int[] keys = GameControls.get(listening).clone();
                    if (keys.length == 0) keys = new int[]{k}; else keys[0] = k;
                    GameControls.set(listening, keys);
                }
                listening = null;
                return true;
            }
            if (isUp(k)) { move(-1); return true; }
            if (isDown(k)) { move(1); return true; }
            if (isRight(k)) {
                if (!inContent) enterContent(); else curRows().get(row).adjust(1);
                return true;
            }
            if (isLeft(k)) {
                if (inContent) {
                    Row r = curRows().get(row);
                    if (r.slider() >= 0f) r.adjust(-1); else leaveContent();
                }
                return true;
            }
            if (isSelect(k)) {
                if (!inContent) {
                    if (leftSel == TAB_COUNT) onBack.run(); else enterContent();
                } else {
                    curRows().get(row).activate();
                }
                return true;
            }
            if (isBack(k)) {
                if (inContent) leaveContent(); else onBack.run();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseMoved(int sx, int sy) {
            if (listening != null) return false;
            Vector2 p = toUi(sx, sy);
            int l = leftHit(p.x, p.y);
            if (l >= 0) {
                leftSel = l;
                if (l < TAB_COUNT) { tab = l; row = 0; }
                inContent = false;
                return true;
            }
            int r = rowHit(p.x, p.y);
            if (r >= 0) { inContent = true; row = r; return true; }
            return false;
        }

        @Override
        public boolean touchDown(int sx, int sy, int pointer, int button) {
            if (listening != null) { listening = null; return true; }
            Vector2 p = toUi(sx, sy);
            int l = leftHit(p.x, p.y);
            if (l >= 0) {
                leftSel = l;
                inContent = false;
                if (l == TAB_COUNT) onBack.run(); else { tab = l; row = 0; }
                return true;
            }
            int r = rowHit(p.x, p.y);
            if (r >= 0) {
                inContent = true;
                row = r;
                Row rw = curRows().get(r);
                if (rw.slider() >= 0f) { dragging = true; dragSlider(p.x); }
                else rw.activate();
                return true;
            }
            return false;
        }

        @Override
        public boolean touchDragged(int sx, int sy, int pointer) {
            if (!dragging) return false;
            dragSlider(toUi(sx, sy).x);
            return true;
        }

        @Override
        public boolean touchUp(int sx, int sy, int pointer, int button) {
            boolean was = dragging;
            dragging = false;
            return was;
        }
    };

    // ------------------------------------------------------------- Screen

    @Override
    public void show() {
        Gdx.input.setInputProcessor(input);
        AudioManager.getInstance().cancelFade();
        if (!AudioManager.getInstance().isMusicPlaying()) {
            String bgmPath = Gdx.files.internal("audio/bgm_title.ogg").exists() ? "audio/bgm_title.ogg" : "audio/bgm_title.mp3";
            AudioManager.getInstance().playMusic(bgmPath, true, 0.7f);
        }
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        AudioManager.getInstance().saveSettings();
    }

    @Override
    public void resize(int width, int height) {
        view.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        tl = Lang.isTL();
        if (toastTimer > 0f) toastTimer -= delta;
        AudioManager.getInstance().update(delta);

        ScreenUtils.clear(Ui.BG);
        view.apply(true);
        ui.batch.setProjectionMatrix(view.getCamera().combined);
        ui.batch.begin();

        // malaking 9-slice board + scroll sa gilid + fleur sa taas
        ui.board(BX, BY, BW, BH, true);
        ui.text(ui.title, text("Mga Setting", "Settings"), 62, 74, Ui.CREAM);
        ui.textRight(ui.tag, text("ENTER = PILIIN   ESC = BUMALIK", "ENTER = SELECT   ESC = BACK"), 578, 73, Ui.MUTE);
        ui.rect(62, 82, 516, 1, Ui.EDGE);

        drawTabs();

        // content panel (9-slice din)
        panel.draw(ui.batch, PX, Ui.up(PY, PH), PW, PH);
        drawContent();

        ui.batch.end();
    }

    private String[] tabNames() {
        return tl
                ? new String[]{"Account", "Layout ng Laro", "Tunog", "Wika"}
                : new String[]{"Account", "Gameplay Layout", "Audio", "Language"};
    }

    private void button(int x, int y, int w, int h, boolean on, boolean gold) {
        for (int s = 0; s < SHADOW.length; s++) ui.rect(x, y + h + s, w, 1, SHADOW[s]);
        int bw = gold ? 2 : 1;
        ui.rect(x, y, w, h, gold ? Ui.GOLD : Ui.EDGE);
        ui.rect(x + bw, y + bw, w - 2 * bw, h - 2 * bw, on ? Ui.BTN_ON : Ui.BTN);
    }

    private void drawTabs() {
        String[] names = tabNames();
        for (int i = 0; i < LEFT_COUNT; i++) {
            boolean focused = !inContent && leftSel == i;
            boolean active = i == tab && i < TAB_COUNT;
            int y = leftY(i);
            button(TAB_X, y, TAB_W, TAB_H, active || focused, focused);
            String label = i < TAB_COUNT ? names[i] : text("Bumalik", "Back");
            ui.text(ui.body, label, TAB_X + 10, y + 18, focused ? Ui.GOLD : Ui.CREAM);
        }
    }

    private String header() {
        switch (tab) {
            case 0: {
                String who = account.username();
                return text("Naka-login bilang: ", "Logged in as: ") + (who == null ? text("Bisita", "Guest") : who);
            }
            case 1:
                return listening != null
                        ? text("Pindutin ang bagong key (Esc = kanselahin)", "Press a new key (Esc = cancel)")
                        : text("Layout ng kontrol", "Control layout");
            case 2: return text("Lakas ng tunog", "Volume");
            default: return text("Wika ng laro", "Game language");
        }
    }

    private void drawContent() {
        ui.text(ui.body, header(), ROW_X + 2, 122, listening != null ? Ui.GOLD : Ui.SUB);

        List<Row> list = curRows();
        for (int i = 0; i < list.size(); i++) {
            Row r = list.get(i);
            int y = ROW_Y0 + i * ROW_STEP;
            boolean on = inContent && i == row;
            button(ROW_X, y, ROW_W, ROW_H, on, on);
            ui.text(ui.body, r.label(), ROW_X + 10, y + 19, on ? Ui.GOLD : Ui.CREAM);

            float s = r.slider();
            if (s >= 0f) {
                drawSlider(y, s, on);
                ui.textRight(ui.body, r.value(), ROW_X + ROW_W - 10, y + 19, on ? Ui.GOLD : Ui.SUB);
            } else {
                String v = r.value();
                if (!v.isEmpty()) ui.textRight(ui.body, v, ROW_X + ROW_W - 10, y + 19, on ? Ui.GOLD : Ui.SUB);
            }
            if (r.checked()) ui.rect(ROW_X + ROW_W - 14, y + 11, 8, 8, Ui.GOLD);
        }

        if (toastTimer > 0f) ui.text(ui.body, toast, ROW_X + 2, 320, Ui.GOLD);
    }

    private void drawSlider(int rowY, float value, boolean on) {
        int ty = rowY + 13;
        int fill = Math.round(TRACK_W * value);
        ui.rect(TRACK_X, ty, TRACK_W, 4, Ui.BG);
        ui.rect(TRACK_X, ty, fill, 4, Ui.GOLD);
        ui.rect(TRACK_X + fill - 2, ty - 4, 4, 12, on ? Ui.CREAM : Ui.EDGE); // knob
    }

    @Override
    public void dispose() {
        panelTex.dispose(); // Ui ay shared; huwag i-dispose dito
    }
}
