package com.dungeonbound.game.lwjgl3;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.dungeonbound.game.DungeonBound;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new DungeonBound(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("DungeonBound: The Filipino Labyrinth");

        // Increase OpenAL audio buffer size and sources to prevent buffer underrun/choppy audio
        configuration.setAudioConfig(32, 2048, 9);

        // Hardware VSync without conflicting foreground FPS sleep loop
        configuration.useVsync(true);
        configuration.setForegroundFPS(0);

        Graphics.DisplayMode dm = Lwjgl3ApplicationConfiguration.getDisplayMode();
        configuration.setDecorated(false);
        configuration.setWindowedMode(dm.width, dm.height);
        configuration.setResizable(true);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        return configuration;
    }
}