package com.dungeonbound.game;

import com.badlogic.gdx.graphics.Color;

/** Patayong listahan ng button. Pareho ang itsura sa language select at pause menu. */
public final class MenuList {
    // 3px na anino sa ilalim ng button (sinukat sa pause menu video)
    private static final Color[] SHADOW = {
            Color.valueOf("462104"), Color.valueOf("3f2214"), Color.valueOf("5e4133")
    };

    public final String[] labels;
    public int sel = 0;
    /** Index ng nakumpirmang pinili; -1 kung wala pa. Nagpapakita ng gintong parisukat. */
    public int done = -1;

    private final int x, y0, w, h, step;

    /** x, y0 = taas-kaliwa ng unang button (y pababa); step = layo ng taas ng isang button sa susunod. */
    public MenuList(String[] labels, int x, int y0, int w, int h, int step) {
        this.labels = labels;
        this.x = x;
        this.y0 = y0;
        this.w = w;
        this.h = h;
        this.step = step;
    }

    public void move(int delta) {
        sel = (sel + delta + labels.length) % labels.length;
    }

    /** px, py ay 640x360 coordinates, y pababa. Ibinabalik ang index o -1. */
    public int hit(float px, float py) {
        for (int i = 0; i < labels.length; i++) {
            int by = y0 + i * step;
            if (px >= x && px < x + w && py >= by && py < by + h) return i;
        }
        return -1;
    }

    public void draw(Ui ui) {
        for (int i = 0; i < labels.length; i++) {
            int by = y0 + i * step;
            boolean on = i == sel;
            for (int s = 0; s < SHADOW.length; s++) ui.rect(x, by + h + s, w, 1, SHADOW[s]);
            int bw = on ? 2 : 1; // 2px na gintong border kapag napili (parehas sa pause menu)
            ui.rect(x, by, w, h, on ? Ui.GOLD : Ui.EDGE);
            ui.rect(x + bw, by + bw, w - 2 * bw, h - 2 * bw, on ? Ui.BTN_ON : Ui.BTN);
            ui.text(ui.body, labels[i], x + 10, by + 19, on ? Ui.GOLD : Ui.CREAM);
            if (i == done) ui.rect(x + w - 14, by + 11, 8, 8, Ui.GOLD);
        }
    }
}
