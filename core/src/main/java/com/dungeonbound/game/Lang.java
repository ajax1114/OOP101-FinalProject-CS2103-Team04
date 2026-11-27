package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public enum Lang {
    TL("tl", "Sandaling Pahinga", "NAKATIGIL",
            "Ipagpatuloy", "Mga Gamit", "Talaan ng Nilalang", "Mga Setting", "Bumalik sa Simula"),
    EN("en", "Taking a Break", "PAUSED",
            "Resume", "Items", "Creature Lore", "Settings", "Back to Start");

    public final String code;
    public final String title;
    public final String tag;
    public final String[] items;

    Lang(String code, String title, String tag, String... items) {
        this.code = code;
        this.title = title;
        this.tag = tag;
        this.items = items;
    }

    public static Lang fromCode(String code, Lang fallback) {
        for (Lang l : values()) if (l.code.equalsIgnoreCase(code)) return l;
        return fallback;
    }

    public static Lang get() {
        Preferences prefs = Gdx.app.getPreferences("dungeonbound_settings");
        String code = prefs.getString("language", "TL");
        return "EN".equalsIgnoreCase(code) ? EN : TL;
    }

    public static boolean isTL() {
        return get() == TL;
    }
}
