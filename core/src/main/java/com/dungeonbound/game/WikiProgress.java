package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Saved na unlocked cards. Una, lahat ay locked.
 *
 * Tawagin kapag natalo ang creature:
 *     if (WikiProgress.unlock("tiyanak")) { ... ipakita "New card unlocked!" ... }
 *
 * Kung may account system ka: WikiProgress.setUser(username) pagkatapos ng login,
 * para hiwalay ang unlocks ng bawat account.
 */
public final class WikiProgress {

    private static final String PREFS = "dungeonbound_wiki";
    private static String user = "guest";

    private WikiProgress() {}

    private static Preferences prefs() { return Gdx.app.getPreferences(PREFS); }
    private static String key(String id) { return user + "." + id; }

    public static void setUser(String username) {
        user = (username == null || username.trim().isEmpty()) ? "guest" : username.trim();
    }

    public static boolean isUnlocked(String id) {
        return prefs().getBoolean(key(id), false);
    }

    /** @return true kung bago itong na-unlock (para sa popup), false kung dati na. */
    public static boolean unlock(String id) {
        Preferences p = prefs();
        if (p.getBoolean(key(id), false)) return false;
        p.putBoolean(key(id), true);
        p.flush();
        return true;
    }

    public static void unlockAll() {
        Preferences p = prefs();
        for (WikiCreature c : WikiCreature.ALL) p.putBoolean(key(c.id), true);
        p.flush();
    }

    public static void resetAll() {
        Preferences p = prefs();
        for (WikiCreature c : WikiCreature.ALL) p.putBoolean(key(c.id), false);
        p.flush();
    }
}
