package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

import java.util.EnumMap;
import java.util.Map;

/**
 * Key bindings ng player (saved). Default: arrows / WASD / Enter / ESC.
 * Para sa custom keys sa Settings screen mo:
 *     GameControls.set(GameControls.Action.LEFT, Input.Keys.Q, Input.Keys.LEFT);
 */
public final class GameControls {

    public enum Action {
        LEFT(Input.Keys.LEFT, Input.Keys.A),
        RIGHT(Input.Keys.RIGHT, Input.Keys.D),
        SELECT(Input.Keys.ENTER, Input.Keys.SPACE),
        BACK(Input.Keys.ESCAPE);

        final int[] defaults;
        Action(int... defaults) { this.defaults = defaults; }
    }

    private static final String PREFS = "dungeonbound_controls";
    private static final Map<Action, int[]> cache = new EnumMap<>(Action.class);

    private GameControls() {}

    public static boolean is(Action action, int keycode) {
        for (int k : get(action)) if (k == keycode) return true;
        return false;
    }

    public static int[] get(Action action) {
        int[] keys = cache.get(action);
        if (keys == null) {
            keys = load(action);
            cache.put(action, keys);
        }
        return keys;
    }

    public static void set(Action action, int... keys) {
        cache.put(action, keys.clone());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) sb.append(i == 0 ? "" : ",").append(keys[i]);
        Preferences p = Gdx.app.getPreferences(PREFS);
        p.putString(action.name(), sb.toString());
        p.flush();
    }

    private static int[] load(Action action) {
        String s = Gdx.app.getPreferences(PREFS).getString(action.name(), "");
        if (s.isEmpty()) return action.defaults;
        try {
            String[] parts = s.split(",");
            int[] keys = new int[parts.length];
            for (int i = 0; i < parts.length; i++) keys[i] = Integer.parseInt(parts[i].trim());
            return keys;
        } catch (NumberFormatException e) {
            return action.defaults;
        }
    }
}
