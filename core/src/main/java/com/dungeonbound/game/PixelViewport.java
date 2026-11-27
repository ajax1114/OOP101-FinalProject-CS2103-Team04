package com.dungeonbound.game;

import com.badlogic.gdx.utils.viewport.FitViewport;

/** 640x360 viewport na naka-FitViewport para puno ang screen habang preserved ang aspect ratio. */
public final class PixelViewport extends FitViewport {
    public static final int W = 640, H = 360;

    public PixelViewport() {
        super(W, H);
    }
}
