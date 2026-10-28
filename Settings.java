package com.burmalda.visuals;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Settings {
    public static boolean targetEsp = true;
    public static boolean jumpCircle = true;
    public static boolean trail = false;
    public static boolean rainbow = false;
    public static float hue = 0.75f;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("burmaldavisuals.properties");
    }

    /** RGB (0xRRGGBB) accent colour; offset shifts the hue in rainbow mode. */
    public static int colorAt(float offset) {
        float h = rainbow ? ((System.currentTimeMillis() % 6000L) / 6000f + offset) % 1f : hue;
        return MathHelper.hsvToRgb(h, 0.7f, 1f);
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (BufferedReader r = Files.newBufferedReader(f)) {
                p.load(r);
            }
            targetEsp = Boolean.parseBoolean(p.getProperty("targetEsp", "true"));
            jumpCircle = Boolean.parseBoolean(p.getProperty("jumpCircle", "true"));
            trail = Boolean.parseBoolean(p.getProperty("trail", "false"));
            rainbow = Boolean.parseBoolean(p.getProperty("rainbow", "false"));
            hue = Math.max(0f, Math.min(1f, Float.parseFloat(p.getProperty("hue", "0.75"))));
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("targetEsp", String.valueOf(targetEsp));
            p.setProperty("jumpCircle", String.valueOf(jumpCircle));
            p.setProperty("trail", String.valueOf(trail));
            p.setProperty("rainbow", String.valueOf(rainbow));
            p.setProperty("hue", String.valueOf(hue));
            try (BufferedWriter w = Files.newBufferedWriter(file())) {
                p.store(w, "BurmaldaVisuals");
            }
        } catch (Exception ignored) {
        }
    }

    private Settings() {
    }
}
