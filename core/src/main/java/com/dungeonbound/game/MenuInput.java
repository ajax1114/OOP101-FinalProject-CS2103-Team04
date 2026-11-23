package com.dungeonbound.game;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;

/** Keyboard (↑ ↓ / W S, Enter / Space, Esc) at mouse (hover, click) para sa isang MenuList. */
public final class MenuInput extends InputAdapter {
    private final MenuList list;
    private final PixelViewport view;
    private final Runnable onConfirm;
    private final Runnable onBack; // puwedeng null
    private final Vector2 tmp = new Vector2();

    public MenuInput(MenuList list, PixelViewport view, Runnable onConfirm, Runnable onBack) {
        this.list = list;
        this.view = view;
        this.onConfirm = onConfirm;
        this.onBack = onBack;
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.UP:
            case Input.Keys.W:
                list.move(-1);
                return true;
            case Input.Keys.DOWN:
            case Input.Keys.S:
                list.move(1);
                return true;
            case Input.Keys.ENTER:
            case Input.Keys.SPACE:
                onConfirm.run();
                return true;
            case Input.Keys.ESCAPE:
                if (onBack != null) onBack.run();
                return onBack != null;
            default:
                return false;
        }
    }

    private int hitAt(int screenX, int screenY) {
        view.unproject(tmp.set(screenX, screenY));
        return list.hit(tmp.x, PixelViewport.H - tmp.y); // y pataas -> y pababa
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        int i = hitAt(screenX, screenY);
        if (i >= 0) list.sel = i;
        return i >= 0;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        int i = hitAt(screenX, screenY);
        if (i < 0) return false;
        list.sel = i;
        onConfirm.run();
        return true;
    }
}
