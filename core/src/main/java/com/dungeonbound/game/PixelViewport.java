package com.dungeonbound.game;

import com.badlogic.gdx.utils.viewport.FitViewport;

/** 640x360 viewport na integer lang ang scale (1x, 2x, 3x...) para pantay ang pixels. */
public final class PixelViewport extends FitViewport {
    public static final int W = 640, H = 360;

    public PixelViewport() {
        super(W, H);
    }

    @Override
    public void update(int screenWidth, int screenHeight, boolean centerCamera) {
        int s = Math.max(1, Math.min(screenWidth / W, screenHeight / H));
        setScreenBounds((screenWidth - W * s) / 2, (screenHeight - H * s) / 2, W * s, H * s);
        apply(centerCamera);
    }
}
