// TitleScreen.java – rewritten to play the Dungeonbound animation pack (libGDX)
// Needs: assets under  core/assets/title/  (atlas/, background/, data/, sprites/glow_*.png)
//        core/assets/audio/ambience_crickets.wav, bgm_title.mp3, sfx_whoosh.wav
// Uses your existing: AudioManager, LoginScreen.
package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TitleScreen implements Screen {
    private enum State { INTRO, IDLE, FOCUS, TRANS }

    // ---- world: y-UP, 1920 wide, tall background 1860 high (780 sky + 1080 menu)
    private static final float W = 1920f, H = 1080f, SKY = 780f, TALL_H = 1860f;
    private static final String DIR = "title/";
    private static final int LOOP = 24;                 // idle loop frames (100 ms each)
    private static final float PRESS_Y = 877f, PRESS_H = 92f, PRESS_X = 807f, PRESS_W = 306f; // START (menu space, y down)

    // transition recipe (data/timeline.json -> transition)
    private static final float TR_MS = 70f;
    private static final float PIVOT_X = 940f, PIVOT_Y_DOWN = 650f;
    private static final float TR_TOTAL = 16f;          // frames: 3 flash + 13 push-in

    private final Game game;
    private final OrthographicCamera cam = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(W, H, cam);
    private final SpriteBatch batch = new SpriteBatch();
    private final List<Texture> textures = new ArrayList<>();

    private TextureAtlas atlas;
    private Texture tall, focusBg, white, vignette, titleTex, btnStartTl;
    private TextureAtlas titleAtlas, capeAtlas;
    private int titleX, titleY, titleW, titleH;      // menu space, y down
    private ShaderProgram screenShader;
    private BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    // data
    private static final class Frame { TextureRegion r; int x, y, w, h, t; }
    private static final class Anim {
        boolean screen;
        final Frame[] byT = new Frame[64];
    }
    private static final class Star { float x, tallY; float[] a; }
    private final Map<String, Anim> anims = new HashMap<>();
    private final Map<String, Texture> glowTex = new HashMap<>();
    private final Map<String, int[]> glowBox = new HashMap<>();      // x,y,w,h (menu space, y down)
    private final Map<String, float[]> glowAlpha = new HashMap<>();
    private final List<Star> stars = new ArrayList<>();
    private float[][][] fireflies;                                   // [n][frame][x,y,a] menu space
    private int[] frameMs, panY;
    private float[] ramp, fadeBlack;
    private int idle0 = 44, panStart = 14, panEnd = 29, readStart = 30;
    private float introMs;

    // runtime
    private State state = State.INTRO;
    private float clockMs = 0f, pressAt = 0f, focusAt = 0f;
    private boolean musicOn, whooshed, left, screenMode;

    public TitleScreen(Game game) {
        this.game = game;
        load();
    }

    // ------------------------------------------------------------------ loading
    private Texture tex(String path, boolean linear) {
        Texture t = new Texture(Gdx.files.internal(DIR + path));
        t.setFilter(linear ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest,
                    linear ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest);
        textures.add(t);
        return t;
    }

    private void load() {
        tall = tex("background/background_tall_nostart_1920x1860.png", true);
        focusBg = tex("background/menu_focus_bg_1920x1080.png", true);
        if (Gdx.files.internal("ui/btn_start_tl.png").exists()) {
            Texture t = new Texture(Gdx.files.internal("ui/btn_start_tl.png"));
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            textures.add(t);
            btnStartTl = t;
        }

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE); pm.fill();
        white = new Texture(pm); pm.dispose();

        // vignette: 256x144 radial black, alpha = 0.75 * min(r,1.4)^2 (scaled by u at draw time)
        Pixmap vp = new Pixmap(256, 144, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 144; y++) for (int x = 0; x < 256; x++) {
            float dx = (x / 255f - 0.5f) * 2f, dy = (y / 143f - 0.5f) * 2f;
            float r = Math.min((float) Math.sqrt(dx * dx + dy * dy), 1.4f);
            vp.drawPixel(x, y, Color.rgba8888(0f, 0f, 0f, Math.min(1f, 0.75f * r * r)));
        }
        vignette = new Texture(vp); vp.dispose();
        vignette.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // premultiplied shader so GL_ONE / GL_ONE_MINUS_SRC_COLOR gives an exact "screen" blend with alpha
        ShaderProgram.pedantic = false;
        String vs = "attribute vec4 a_position;attribute vec4 a_color;attribute vec2 a_texCoord0;"
                + "uniform mat4 u_projTrans;varying vec4 v_color;varying vec2 v_texCoords;"
                + "void main(){v_color=a_color;v_color.a=v_color.a*(255.0/254.0);v_texCoords=a_texCoord0;"
                + "gl_Position=u_projTrans*a_position;}";
        String fs = "#ifdef GL_ES\nprecision mediump float;\n#endif\n"
                + "varying vec4 v_color;varying vec2 v_texCoords;uniform sampler2D u_texture;"
                + "void main(){vec4 c=texture2D(u_texture,v_texCoords)*v_color;gl_FragColor=vec4(c.rgb*c.a,c.a);}";
        screenShader = new ShaderProgram(vs, fs);
        if (!screenShader.isCompiled()) {
            Gdx.app.error("TitleScreen", "screen shader: " + screenShader.getLog());
            screenShader = null;
        }

        atlas = new TextureAtlas(Gdx.files.internal(DIR + "atlas/fx.atlas"));
        JsonReader jr = new JsonReader();

        // --- sprites.json -> animations (positions are in menu space, y down, top-left)
        JsonValue sp = jr.parse(Gdx.files.internal(DIR + "data/sprites.json"));
        readAnims(sp.get("animations"), atlas);
        // --- new title (static + gold letter sweep + shimmer + FILIPINO pulse)
        titleAtlas = new TextureAtlas(Gdx.files.internal(DIR + "atlas/title.atlas"));
        JsonValue tj = jr.parse(Gdx.files.internal(DIR + "data/title.json"));
        JsonValue st = tj.get("static");
        titleTex = tex(st.getString("file"), false);
        titleX = st.getInt("x"); titleY = st.getInt("y"); titleW = st.getInt("w"); titleH = st.getInt("h");
        readAnims(tj.get("animations"), titleAtlas);

        // --- cape (separate sprite, pixel-snapped sway)
        capeAtlas = new TextureAtlas(Gdx.files.internal(DIR + "atlas/cape.atlas"));
        readAnims(jr.parse(Gdx.files.internal(DIR + "data/cape.json")).get("animations"), capeAtlas);

        for (JsonValue g = sp.get("glow_sprites").child; g != null; g = g.next) {
            glowTex.put(g.name, tex(g.getString("file"), true));
            glowBox.put(g.name, new int[]{g.getInt("x"), g.getInt("y"), g.getInt("w"), g.getInt("h")});
        }

        // --- timeline.json
        JsonValue tl = jr.parse(Gdx.files.internal(DIR + "data/timeline.json"));
        frameMs = tl.get("frame_ms").asIntArray();
        panY = tl.get("camera").get("camera_y_per_pan_frame").asIntArray();
        ramp = tl.get("torches_ignite").get("glow_ramp_per_frame").asFloatArray();
        fadeBlack = tl.get("intro").get("fade_black_opacity").asFloatArray();
        idle0 = tl.get("tags").get("idle_loop").asIntArray()[0];
        panStart = tl.get("tags").get("camera_pan").asIntArray()[0];
        panEnd = tl.get("tags").get("camera_pan").asIntArray()[1];
        readStart = tl.get("tags").get("title_read").asIntArray()[0];
        introMs = 0f;
        for (int i = 0; i < idle0; i++) introMs += frameMs[i];

        // --- particles.json
        JsonValue pj = jr.parse(Gdx.files.internal(DIR + "data/particles.json"));
        for (JsonValue t = pj.get("torch_glow").child; t != null; t = t.next)
            glowAlpha.put(t.getString("sprite"), t.get("alpha_per_frame").asFloatArray());
        addStars(pj.get("stars_menu"));
        addStars(pj.get("stars_sky_extension"));
        JsonValue ff = pj.get("fireflies").get("positions_per_frame");
        fireflies = new float[ff.size][][];
        int i = 0;
        for (JsonValue one = ff.child; one != null; one = one.next, i++) {
            fireflies[i] = new float[one.size][3];
            int k = 0;
            for (JsonValue p = one.child; p != null; p = p.next, k++) {
                fireflies[i][k][0] = p.getFloat(0); fireflies[i][k][1] = p.getFloat(1); fireflies[i][k][2] = p.getFloat(2);
            }
        }

        // --- font (your FreeType font if present, 32px para sakto ang lapad ng Tagalog/English prompt)
        try {
            if (Gdx.files.internal("fonts/font_body.ttf").exists()) {
                FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/font_body.ttf"));
                FreeTypeFontParameter par = new FreeTypeFontParameter();
                par.size = 32;
                par.borderWidth = 2; par.borderColor = new Color(0, 0, 0, 0.85f);
                font = gen.generateFont(par);
                gen.dispose();
            }
        } catch (Exception e) {
            Gdx.app.error("TitleScreen", "font: " + e);
        }
        if (font == null) { font = new BitmapFont(); font.getData().setScale(2.2f); }
    }

    private void readAnims(JsonValue animsNode, TextureAtlas at) {
        for (JsonValue a = animsNode.child; a != null; a = a.next) {
            Anim an = new Anim();
            an.screen = "screen".equals(a.getString("blend"));
            for (JsonValue f = a.get("frames").child; f != null; f = f.next) {
                Frame fr = new Frame();
                fr.r = at.findRegion(a.name, f.getInt("index"));
                fr.x = f.getInt("x"); fr.y = f.getInt("y"); fr.w = f.getInt("w"); fr.h = f.getInt("h"); fr.t = f.getInt("t");
                if (fr.r != null && fr.t >= 0 && fr.t < 64) an.byT[fr.t] = fr;
            }
            anims.put(a.name, an);
        }
    }

    private void addStars(JsonValue arr) {
        for (JsonValue s = arr.child; s != null; s = s.next) {
            Star st = new Star();
            st.x = s.getFloat("x"); st.tallY = s.getFloat("tall_y");
            st.a = s.get("alpha_per_frame").asFloatArray();
            stars.add(st);
        }
    }

    // ------------------------------------------------------------------ input / state
    /** any key / tap: skip intro, then (IDLE) blur the background and show START. */
    private boolean press() {
        if (state == State.INTRO) { clockMs = introMs; state = State.IDLE; return true; }   // skip intro
        if (state != State.IDLE) return false;
        state = State.FOCUS;
        focusAt = clockMs;
        return true;
    }

    /** START pressed: flash, then push into the tunnel. */
    private void startPressed() {
        if (state != State.FOCUS) return;
        state = State.TRANS;
        pressAt = clockMs;
    }

    private boolean hitStart(int sx, int sy) {
        Vector2 v = viewport.unproject(new Vector2(sx, sy));
        float mx = v.x, my = TALL_H - v.y - SKY;                      // menu space, y down
        return mx >= PRESS_X - 10 && mx <= PRESS_X + PRESS_W + 10 && my >= PRESS_Y - 10 && my <= PRESS_Y + PRESS_H + 10;
    }

    // ------------------------------------------------------------------ drawing helpers
    private void mode(boolean screen) {
        if (screen == screenMode) return;
        screenMode = screen;
        if (screen) {
            if (screenShader != null) batch.setShader(screenShader);
            batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR);
        } else {
            batch.setShader(null);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /** menu-space (y down) top-left sprite frame -> world. */
    private void drawAnim(String name, int t, float alpha) {
        Anim a = anims.get(name);
        if (a == null || alpha <= 0.002f) return;
        Frame f = a.byT[t];
        if (f == null) return;
        mode(a.screen);
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(f.r, f.x, TALL_H - (f.y + SKY + f.h), f.w, f.h);
    }

    /** square centred on tall-space (x, yDown). */
    private void box(float cx, float cyDown, float size, float r, float g, float b, float a) {
        if (a <= 0.002f) return;
        batch.setColor(r, g, b, a);
        batch.draw(white, cx - size / 2f, TALL_H - (cyDown + size / 2f), size, size);
    }

    // ------------------------------------------------------------------ Screen
    @Override public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (state == State.FOCUS) {
                    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) { startPressed(); return true; }
                    return false;
                }
                return press();
            }
            @Override public boolean touchDown(int x, int y, int p, int b) {
                if (state == State.FOCUS) { if (hitStart(x, y)) { startPressed(); return true; } return false; }
                return press();
            }
        });
        // Balanced ambience volume para hindi muffled at hindi magkaroon ng phase cancellation sa BGM
        String ambPath = Gdx.files.internal("audio/amb_crickets.wav").exists() ? "audio/amb_crickets.wav" : "audio/ambience_crickets.wav";
        AudioManager.getInstance().playAmbience(ambPath, true, 0.18f);
    }

    @Override public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);
        clockMs += delta * 1000f;

        // ---- which animation frame are we on? (plus sub-frame frac for smooth 60fps motion)
        int frame;
        float frac = 0f;
        if (state == State.INTRO && clockMs >= introMs) state = State.IDLE;
        if (state == State.INTRO) {
            float acc = 0f; frame = idle0 - 1;
            for (int i = 0; i < idle0; i++) {
                if (clockMs < acc + frameMs[i]) {
                    frame = i;
                    frac = Math.max(0f, Math.min(1f, (clockMs - acc) / frameMs[i]));
                    break;
                }
                acc += frameMs[i];
            }
        } else {
            float idleClock = (clockMs - introMs) / 100f;
            frame = idle0 + ((int) idleClock) % LOOP;
            frac = idleClock - (float) Math.floor(idleClock);
        }
        int tl = Math.floorMod(frame - idle0, LOOP);
        int tlNext = (tl + 1) % LOOP;

        // ---- audio cues: gamitin ang bgm_title.mp3 (44.1kHz) para smooth at hindi choppy
        if (!musicOn && clockMs >= 1200f) {
            musicOn = true;
            AudioManager.getInstance().playTitleMusic();
        }
        float kk = -1f;
        if (state == State.TRANS) {
            kk = (clockMs - pressAt) / TR_MS;
            if (!whooshed && (clockMs - pressAt) >= 180f) { whooshed = true; AudioManager.getInstance().playSfx("audio/sfx_whoosh.wav", 1.0f); }
            if (kk >= TR_TOTAL && !left) {
                left = true;
                AudioManager.getInstance().stopAmbience();
                game.setScreen(new LoginScreen(game));
                return;
            }
        }

        // ---- camera y & torch glow ramp (smoothly interpolated between pan frames)
        float camY;
        if (frame < panStart) camY = 0f;
        else if (frame < panEnd) {
            int idx = frame - panStart;
            camY = panY[idx] + (panY[idx + 1] - panY[idx]) * frac;
        } else camY = SKY;

        float torchRamp;
        if (frame < panStart) torchRamp = 0f;
        else if (frame < panEnd) {
            int idx = frame - panStart;
            torchRamp = ramp[idx] + (ramp[idx + 1] - ramp[idx]) * frac;
        } else torchRamp = 1f;

        // ---- transition parameters
        float focus = 0f, scale = 1f, u = 0f, fadeOut = 0f, startA = 0f, promptA = 0f;
        if (state == State.IDLE) promptA = 1f;
        if (state == State.FOCUS) {
            float ms = clockMs - focusAt;
            float f = Math.min(ms / 500f, 1f);
            focus = 0.5f - 0.5f * (float) Math.cos(Math.PI * f);              // blur-focus 0.5 s
            startA = Math.max(0f, Math.min(1f, (ms - 300f) / 400f));          // START fades in after 0.3 s
            promptA = Math.max(0f, 1f - ms / 200f);
        }
        if (state == State.TRANS) {
            focus = 1f; startA = 1f;
            float kp = kk - 3f;                                               // push-in starts after the 3 flash frames
            if (kp > 0f) { u = Math.min(kp / 12f, 1f); scale = (float) Math.exp(Math.log(7.0) * u * u); }
            fadeOut = Math.max(0f, Math.min(1f, (kp - 6f) / 5f));
        }
        float fx = 1f - focus;                       // animated layers fade as the focus bg takes over

        // ---- camera (keeps PIVOT fixed on screen while zooming)
        float z = 1f / scale;
        float cxW = W / 2f, cyW = TALL_H - (camY + H / 2f);
        float px = PIVOT_X, py = TALL_H - (SKY + PIVOT_Y_DOWN);
        viewport.apply();
        cam.zoom = z;
        cam.position.set(px + (cxW - px) * z, py + (cyW - py) * z, 0f);
        cam.update();

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        screenMode = true; mode(false);              // force normal state at frame start

        // base art
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(tall, 0f, 0f, W, TALL_H);
        if (focus > 0f) {                            // blur-focus: menu -> focus comp
            batch.setColor(1f, 1f, 1f, focus);
            batch.draw(focusBg, 0f, TALL_H - (SKY + H), W, H);
        }

        // wak-wak bird (intro only, tall space, smooth flight)
        if (state == State.INTRO && frame >= 6) {
            float fCont = frame + frac;
            float bx = 500f + (fCont - 6f) * 78f;
            if (bx <= 2100f) {
                float by = 178f + SKY - 24f + 14f * (float) Math.sin(0.9f * fCont);
                Anim bird = anims.get("wakwak_bird");
                Frame f = (bird == null) ? null : bird.byT[((frame / 2) % 4)];
                if (f != null) {
                    mode(false);
                    batch.setColor(1f, 1f, 1f, 1f);
                    batch.draw(f.r, bx + f.x, TALL_H - (by + f.y + f.h), f.w, f.h);
                }
            }
        }

        // animated background layers (moon halo, torch glow, cape, kapre)
        drawAnim("moon_halo_dither", tl, fx * 0.75f); // Atmospheric moonlight halo (cleaned of arch bakat in atlas)

        float tSec = clockMs * 0.001f;

        // Torches with organic fast & slow glow dynamics
        float fastFlickerL = 1f + 0.06f * (float) Math.sin(tSec * 13.5f) + 0.04f * (float) Math.cos(tSec * 21.0f);
        float fastFlickerR = 1f + 0.06f * (float) Math.sin(tSec * 15.2f + 1.2f) + 0.04f * (float) Math.cos(tSec * 19.3f + 2.0f);

        for (Map.Entry<String, float[]> e : glowAlpha.entrySet()) {
            int[] b = glowBox.get(e.getKey());
            mode(true);
            float g0 = e.getValue()[tl], g1 = e.getValue()[tlNext];
            float ga = (g0 + (g1 - g0) * frac) * torchRamp * fx;
            String k = e.getKey();
            float flicker = (k.contains("L")) ? fastFlickerL : fastFlickerR;
            batch.setColor(1f, 1f, 1f, Math.min(1f, ga * flicker));
            batch.draw(glowTex.get(e.getKey()), b[0], TALL_H - (b[1] + SKY + b[3]), b[2], b[3]);
        }
        drawAnim("cape_sway", tl, 1f);               // cape stays sharp and visible during the blur
        drawAnim("embers", tl, fx);                  // background embers
        drawAnim("kapre_cigar", tl, fx);

        // stars + fireflies + torch embers (screen blend)
        mode(true);

        // Torch embers (natural combination of fast sparks and slow floating embers)
        if (torchRamp > 0.05f && fx > 0.05f) {
            // Front-right torch (x ~ 1218)
            float[] speedsR = { 0.85f, 0.42f, 0.28f, 0.65f }; // fast sparks & slow lingering embers
            float[] heightsR = { 50f, 40f, 28f, 45f };
            float[] sizesR = { 2.5f, 3.5f, 3.0f, 2.0f };
            for (int i = 0; i < 4; i++) {
                float p = (tSec * speedsR[i] + i * 0.25f) % 1f;
                float drift = (float) Math.sin(tSec * (2.2f + i * 0.8f) + i * 1.7f) * (5f + i * 1.5f);
                float ex = 1218f + drift;
                float ey = (372f - p * heightsR[i]) + SKY;
                float ea = (float) Math.sin(p * Math.PI) * 0.85f * torchRamp * fx;
                box(ex, ey, sizesR[i], 1f, 0.75f, 0.28f, ea);
            }
            // Front-left torch matching embers (x ~ 654)
            for (int i = 0; i < 3; i++) {
                float p = (tSec * speedsR[i + 1] + i * 0.33f + 0.15f) % 1f;
                float drift = (float) Math.sin(tSec * (2.4f + i) + i * 2.3f) * 6f;
                float ex = 654f + drift;
                float ey = (372f - p * heightsR[i + 1]) + SKY;
                float ea = (float) Math.sin(p * Math.PI) * 0.85f * torchRamp * fx;
                box(ex, ey, sizesR[i + 1], 1f, 0.75f, 0.28f, ea);
            }
        }

        // Stars (restored to original full count & classic brightness, with smooth 60fps interpolation)
        for (Star s : stars) {
            float a0 = s.a[tl], a1 = s.a[tlNext];
            float rawA = a0 + (a1 - a0) * frac;
            float a = rawA * fx;
            if (a <= 0.002f) continue;
            box(s.x, s.tallY, 6f, 1f, 250f / 255f, 225f / 255f, a);
            if (rawA > 0.45f) {
                float c = a * 0.7f;
                box(s.x - 9f, s.tallY, 5f, 220f / 255f, 235f / 255f, 1f, c);
                box(s.x + 9f, s.tallY, 5f, 220f / 255f, 235f / 255f, 1f, c);
                box(s.x, s.tallY - 9f, 5f, 220f / 255f, 235f / 255f, 1f, c);
                box(s.x, s.tallY + 9f, 5f, 220f / 255f, 235f / 255f, 1f, c);
            }
        }

        // Fireflies (enhanced mystery vibes: 14 well-distributed glowing lights across the cemetery)
        if (frame >= panStart) {
            final int[] activeFf = { 0, 3, 5, 7, 10, 11, 16, 1, 2, 4, 6, 8, 9, 15 };
            final float[][] ffOffsets = {
                {   0f,   0f }, // 0: (272, 636) - mid-left
                {  60f,  40f }, // 3: (476, 752)
                { -30f,  60f }, // 5: (338, 676)
                {-120f, -20f }, // 7: far-left trees
                {  20f,  30f }, // 10: bottom-left grass
                {  30f, -40f }, // 11: low-left tombstones
                {  50f, -60f }, // 16: mid-left field
                {   0f,   0f }, // 1: (1620, 684) - mid-right
                {  60f, -30f }, // 2: (1776, 706) - far-right
                { -40f, -30f }, // 4: bottom-right
                { -60f,  30f }, // 6: mid-right ground
                {  30f, -50f }, // 8: lower-right field
                { -50f,  20f }, // 9: mid-right gravestones
                { -60f,  30f }  // 15: inner-right trees
            };
            for (int k = 0; k < activeFf.length; k++) {
                int fi = activeFf[k];
                if (fi >= fireflies.length) continue;
                float[][] ffly = fireflies[fi];
                float[] p0 = ffly[tl], p1 = ffly[tlNext];
                float ox = (k < ffOffsets.length) ? ffOffsets[k][0] : 0f;
                float oy = (k < ffOffsets.length) ? ffOffsets[k][1] : 0f;
                float x = p0[0] + (p1[0] - p0[0]) * frac + ox;
                float y = (p0[1] + (p1[1] - p0[1]) * frac + oy) + SKY;
                float a = (p0[2] + (p1[2] - p0[2]) * frac) * fx * 0.95f;
                box(x, y, 5.5f, 215f / 255f, 1f, 135f / 255f, a);
                float h = a * 0.40f;                               // glowing halo
                box(x - 5f, y, 4.5f, 150f / 255f, 220f / 255f, 110f / 255f, h);
                box(x + 5f, y, 4.5f, 150f / 255f, 220f / 255f, 110f / 255f, h);
                box(x, y - 5f, 4.5f, 150f / 255f, 220f / 255f, 110f / 255f, h);
                box(x, y + 5f, 4.5f, 150f / 255f, 220f / 255f, 110f / 255f, h);
            }
        }

        // title (static) + animations - drawn AFTER environment & embers so embers never bleed over the title letters
        float titleA = (kk >= 0f) ? Math.max(0f, 1f - kk / 5f) : 1f;
        if (titleA > 0f) {
            mode(false);
            batch.setColor(1f, 1f, 1f, titleA);
            batch.draw(titleTex, titleX, TALL_H - (SKY + titleY + titleH), titleW, titleH);
            if (frame >= readStart && frame < idle0) drawAnim("ttl_highlight", frame - readStart, titleA);   // gold letter sweep
            drawAnim("ttl_pulse", tl, titleA);                                                                // FILIPINO glow
            drawAnim("ttl_shimmer", tl, titleA);                                                              // light sweep
        }

        // START button (appears after the blur)
        if (startA > 0f) {
            if (Lang.isTL() && btnStartTl != null) {
                mode(false);
                batch.setColor(1f, 1f, 1f, startA);
                batch.draw(btnStartTl, PRESS_X, TALL_H - (PRESS_Y + SKY + PRESS_H), PRESS_W, PRESS_H);
            } else {
                drawAnim("start_button", 0, startA);
            }
            drawAnim("start_glint_pulse", tl, startA);
        }

        // START press flash (3 frames)
        if (kk >= 0f && kk < 3f) drawAnim("start_press_flash", (int) kk, 1f);

        // vignette + fade (cover the whole camera view)
        float vx = cam.position.x - W / 2f * z, vy = cam.position.y - H / 2f * z;
        mode(false);
        if (u > 0f) {
            batch.setColor(1f, 1f, 1f, u);
            batch.draw(vignette, vx, vy, W * z, H * z);
        }
        float black = 0f;
        if (state == State.INTRO && frame < fadeBlack.length) {
            float b0 = fadeBlack[frame];
            float b1 = (frame + 1 < fadeBlack.length) ? fadeBlack[frame + 1] : 0f;
            black = (b0 + (b1 - b0) * frac) / 255f;
        }
        black = Math.max(black, fadeOut);
        if (black > 0f) {
            batch.setColor(0f, 0f, 0f, black);
            batch.draw(white, vx - 50f, vy - 50f, W * z + 100f, H * z + 100f);
        }

        // Prompt at the bottom, localized according to chosen language
        if (promptA > 0f && frame >= readStart) {
            float a = (0.55f + 0.45f * (float) Math.sin(clockMs / 1000f * 3.2f)) * promptA;
            font.setColor(1f, 1f, 1f, a);
            layout.setText(font, Strings.titlePrompt());
            float tx = (W - layout.width) / 2f;
            float ty = (cam.position.y - H / 2f) + 56f + layout.height;   // bottom of the screen
            font.draw(batch, layout, tx, ty);
        }
        batch.end();
    }

    @Override public void resize(int w, int h) { viewport.update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override public void dispose() {
        batch.dispose();
        if (atlas != null) atlas.dispose();
        if (titleAtlas != null) titleAtlas.dispose();
        if (capeAtlas != null) capeAtlas.dispose();
        if (screenShader != null) screenShader.dispose();
        if (white != null) white.dispose();
        if (vignette != null) vignette.dispose();
        if (font != null) font.dispose();
        for (Texture t : textures) t.dispose();
        textures.clear();
        glowTex.clear();
        anims.clear();
    }
}
