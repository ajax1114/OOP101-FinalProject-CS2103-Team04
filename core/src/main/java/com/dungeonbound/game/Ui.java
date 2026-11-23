package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Lahat ng shared na assets (texture, font, batch) at drawing helpers.
 * Lahat ng coordinates dito ay 640x360, ORIGIN SA TAAS-KALIWA, y pababa (tulad ng HTML).
 */
public final class Ui implements Disposable {
    public static final Color BG = Color.valueOf("181818");
    public static final Color CREAM = Color.valueOf("f1e2c9");
    public static final Color SUB = Color.valueOf("c9a77f");
    public static final Color MUTE = Color.valueOf("b79f84");
    public static final Color GOLD = Color.valueOf("f5c964");
    public static final Color BTN = Color.valueOf("673f2b");
    public static final Color BTN_ON = Color.valueOf("8a612d");
    public static final Color EDGE = Color.valueOf("b3845f");

    public final SpriteBatch batch = new SpriteBatch();
    public final BitmapFont body, title, tag;
    public final Texture menuBoard;

    private final Texture frame, frameClean, fleur, scrollLCap, scrollLBot, scrollRCap, scrollRBot, px;
    private final TextureRegion scrollLLoopRegion, scrollLTailRegion, scrollRLoopRegion, scrollRTailRegion;
    // 9-slice: left 8, right 8, top 15, bottom 15 (parehas sa HTML)
    private final NinePatch patchFleur, patchClean;
    private final GlyphLayout layout = new GlyphLayout();

    public Ui() {
        frame = tex("ui/frame.png");
        frameClean = tex("ui/frame_clean.png"); // walang bukol sa taas; para sa board na walang fleur
        fleur = tex("ui/fleur.png");
        scrollLCap = tex("ui/scroll_left_cap.png");
        scrollLBot = tex("ui/scroll_left_bottom.png");
        scrollRCap = tex("ui/scroll_right_cap.png");
        scrollRBot = tex("ui/scroll_right_bottom.png");
        menuBoard = tex("ui/menu_board.png");
        // Loop segment (0..60) at tail segment (60..wakas) para tuluy-tuloy ang scroll pababa
        scrollLLoopRegion = new TextureRegion(scrollLBot, 0, 0, scrollLBot.getWidth(), 60);
        scrollLTailRegion = new TextureRegion(scrollLBot, 0, 60, scrollLBot.getWidth(), scrollLBot.getHeight() - 60);
        scrollRLoopRegion = new TextureRegion(scrollRBot, 0, 0, scrollRBot.getWidth(), 60);
        scrollRTailRegion = new TextureRegion(scrollRBot, 0, 60, scrollRBot.getWidth(), scrollRBot.getHeight() - 60);
        patchFleur = new NinePatch(frame, 8, 8, 15, 15);
        patchClean = new NinePatch(frameClean, 8, 8, 15, 15);

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        px = new Texture(pm);
        pm.dispose();

        body = font("fonts/PixelifySans-Regular.ttf", 12);
        title = font("fonts/PixelifySans-SemiBold.ttf", 16);
        tag = font("fonts/PixelifySans-Regular.ttf", 7); // maliit na "NAKATIGIL"; subukan 8 kung malabo
    }

    private static Texture tex(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        return t;
    }

    private static BitmapFont font(String path, int size) {
        FileHandle f = Gdx.files.internal(path);
        if (!f.exists()) {
            throw new GdxRuntimeException("Walang font: " + path
                    + " (kunin ang Pixelify Sans sa fonts.google.com/specimen/Pixelify+Sans)");
        }
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(f);
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = size;
        p.mono = true; // walang anti-aliasing = malinaw na pixel
        p.minFilter = TextureFilter.Nearest;
        p.magFilter = TextureFilter.Nearest;
        BitmapFont bf = gen.generateFont(p);
        gen.dispose();
        return bf;
    }

    /** Y pababa (taas ng object) -> y pataas ng libGDX. */
    public static int up(int topY, int h) {
        return PixelViewport.H - topY - h;
    }

    public void rect(int x, int topY, int w, int h, Color c) {
        batch.setColor(c);
        batch.draw(px, x, up(topY, h), w, h);
        batch.setColor(Color.WHITE);
    }

    /** Board na may 9-slice frame, mga scroll sa gilid na tuluy-tuloy pababa, at (opsyonal) fleur sa taas. */
    public void board(int fx, int fy, int fw, int fh, boolean withFleur) {
        (withFleur ? patchFleur : patchClean).draw(batch, fx, up(fy, fh), fw, fh);
        int w = scrollLCap.getWidth();
        // Left scroll column
        drawScrollColumn(scrollLCap, scrollLLoopRegion, scrollLTailRegion, fx - w, fy + 1, fh - 2);
        // Right scroll column
        drawScrollColumn(scrollRCap, scrollRLoopRegion, scrollRTailRegion, fx + fw, fy + 1, fh - 2);

        if (withFleur) {
            int ox = fx + Math.round((fw - fleur.getWidth()) / 2f) - 1;
            batch.draw(fleur, ox, up(fy - 35, fleur.getHeight()));
        }
    }

    private void drawScrollColumn(Texture cap, TextureRegion loop, TextureRegion tail, int x, int startY, int totalH) {
        batch.draw(cap, x, up(startY, cap.getHeight()));
        int currY = startY + cap.getHeight();
        int tailH = tail.getRegionHeight();
        int targetEnd = startY + totalH;

        while (currY + tailH < targetEnd) {
            batch.draw(loop, x, up(currY, loop.getRegionHeight()));
            currY += loop.getRegionHeight();
        }
        batch.draw(tail, x, up(currY, tailH));
    }

    /** Ang baselineTop ay y (pababa) ng baseline ng text. */
    public void text(BitmapFont f, String s, int x, int baselineTop, Color c) {
        f.setColor(c);
        // Sa libGDX, ang y ng draw() ay tuktok ng malalaking letra, kaya + capHeight.
        f.draw(batch, s, x, PixelViewport.H - baselineTop + f.getCapHeight());
    }

    public void textCenter(BitmapFont f, String s, int cx, int baselineTop, Color c) {
        layout.setText(f, s);
        text(f, s, Math.round(cx - layout.width / 2f), baselineTop, c);
    }

    public void textRight(BitmapFont f, String s, int rightX, int baselineTop, Color c) {
        layout.setText(f, s);
        text(f, s, Math.round(rightX - layout.width), baselineTop, c);
    }

    @Override
    public void dispose() {
        batch.dispose();
        body.dispose();
        title.dispose();
        tag.dispose();
        for (Texture t : new Texture[]{frame, frameClean, fleur, scrollLCap, scrollLBot, scrollRCap, scrollRBot, menuBoard, px}) {
            t.dispose();
        }
    }
}
