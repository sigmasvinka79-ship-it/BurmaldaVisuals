package com.burmalda.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MenuScreen extends Screen {
    private record Toggle(String name, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {
    }

    private static final int W = 270, ROW = 32, HEAD = 50, PAD = 14;

    private final List<Toggle> toggles = List.of(
            new Toggle("Target ESP", "Кольцо вокруг цели атаки", () -> Settings.targetEsp, v -> Settings.targetEsp = v),
            new Toggle("Jump Circle", "Круг на земле при прыжке", () -> Settings.jumpCircle, v -> Settings.jumpCircle = v),
            new Toggle("Trail", "Светящийся след за игроком", () -> Settings.trail, v -> Settings.trail = v),
            new Toggle("Rainbow", "Переливающийся цвет эффектов", () -> Settings.rainbow, v -> Settings.rainbow = v));

    private final float[] anim = new float[toggles.size()];
    private float open = 0f;
    private long last = System.nanoTime();
    private boolean dragging = false;

    public MenuScreen() {
        super(Text.literal("BurmaldaVisuals"));
        for (int i = 0; i < anim.length; i++) anim[i] = toggles.get(i).get().get() ? 1f : 0f;
    }

    private int ph() {
        return HEAD + toggles.size() * ROW + 44 + PAD;
    }

    private int px() {
        return (width - W) / 2;
    }

    private int py() {
        return (height - ph()) / 2;
    }

    private int sliderX() {
        return px() + PAD;
    }

    private int sliderW() {
        return W - PAD * 2;
    }

    private int sliderY() {
        return py() + HEAD + toggles.size() * ROW + 20;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.nanoTime();
        float dt = (now - last) / 1e9f;
        last = now;
        open = Math.min(1f, open + dt * 5f);
        float e = 1f - (1f - open) * (1f - open);
        for (int i = 0; i < anim.length; i++) {
            float goal = toggles.get(i).get().get() ? 1f : 0f;
            anim[i] += (goal - anim[i]) * Math.min(1f, dt * 14f);
        }
        int accent = Settings.colorAt(0f);

        ctx.fill(0, 0, width, height, argb(0x000000, 0.5f * e));

        int x = px(), y = py(), h = ph();
        float cx = x + W / 2f, cy = y + h / 2f;
        float s = 0.92f + 0.08f * e;
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(cx, cy, 0);
        ms.scale(s, s, 1f);
        ms.translate(-cx, -cy, 0);

        rrect(ctx, x - 2, y - 2, W + 4, h + 4, 10, argb(accent, 0.28f * e));
        rrect(ctx, x, y, W, h, 8, argb(0x15131F, 0.97f * e));

        ms.push();
        ms.translate(x + PAD, y + 12, 0);
        ms.scale(1.35f, 1.35f, 1f);
        ctx.drawText(textRenderer, "BurmaldaVisuals", 0, 0, argb(accent, e), true);
        ms.pop();
        ms.push();
        ms.translate(x + PAD, y + 29, 0);
        ms.scale(0.8f, 0.8f, 1f);
        ctx.drawText(textRenderer, "Right Shift — закрыть меню", 0, 0, argb(0x8E8AA6, e), false);
        ms.pop();
        ctx.fill(x + PAD, y + HEAD - 6, x + W - PAD, y + HEAD - 5, argb(accent, 0.8f * e));

        for (int i = 0; i < toggles.size(); i++) {
            Toggle t = toggles.get(i);
            int ry = y + HEAD + i * ROW;
            boolean hover = mx >= x + PAD - 4 && mx <= x + W - PAD + 4 && my >= ry && my < ry + ROW - 2;
            if (hover) rrect(ctx, x + PAD - 4, ry, W - PAD * 2 + 8, ROW - 2, 6, argb(0xFFFFFF, 0.06f * e));

            ctx.drawText(textRenderer, t.name(), x + PAD, ry + 5, argb(0xFFFFFF, e), false);
            ms.push();
            ms.translate(x + PAD, ry + 17, 0);
            ms.scale(0.75f, 0.75f, 1f);
            ctx.drawText(textRenderer, t.desc(), 0, 0, argb(0x8E8AA6, e), false);
            ms.pop();

            int sw = 30, sh = 14;
            int sx = x + W - PAD - sw;
            int sy = ry + (ROW - 2 - sh) / 2;
            rrect(ctx, sx, sy, sw, sh, 7, argb(lerpRgb(0x3A3750, accent, anim[i]), e));
            int kx = sx + 2 + Math.round((sw - sh) * anim[i]);
            rrect(ctx, kx, sy + 2, sh - 4, sh - 4, 5, argb(0xFFFFFF, e));
        }

        int sy0 = y + HEAD + toggles.size() * ROW;
        ctx.drawText(textRenderer, "Color", x + PAD, sy0 + 4, argb(0xFFFFFF, e), false);
        int bx = sliderX(), bw = sliderW(), by = sliderY();
        for (int i = 0; i < bw; i++) {
            ctx.fill(bx + i, by, bx + i + 1, by + 6, argb(MathHelper.hsvToRgb(i / (float) bw, 0.7f, 1f), e));
        }
        int knob = bx + Math.round(Settings.hue * (bw - 1));
        rrect(ctx, knob - 3, by - 3, 6, 12, 3, argb(0xFFFFFF, e));

        ms.pop();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int x = px(), y = py();
            for (int i = 0; i < toggles.size(); i++) {
                int ry = y + HEAD + i * ROW;
                if (mx >= x + PAD - 4 && mx <= x + W - PAD + 4 && my >= ry && my < ry + ROW - 2) {
                    Toggle t = toggles.get(i);
                    t.set().accept(!t.get().get());
                    Settings.save();
                    return true;
                }
            }
            int by = sliderY();
            if (mx >= sliderX() - 4 && mx <= sliderX() + sliderW() + 4 && my >= by - 6 && my <= by + 12) {
                dragging = true;
                setHue(mx);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            setHue(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            Settings.save();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (BurmaldaVisualsClient.menuKey != null && BurmaldaVisualsClient.menuKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void setHue(double mx) {
        Settings.hue = (float) MathHelper.clamp((mx - sliderX()) / (sliderW() - 1), 0.0, 1.0);
    }

    private static int argb(int rgb, float a) {
        int al = Math.max(5, Math.min(255, Math.round(a * 255f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    private static int lerpRgb(int a, int b, float t) {
        int r = Math.round(MathHelper.lerp(t, (a >> 16) & 255, (b >> 16) & 255));
        int g = Math.round(MathHelper.lerp(t, (a >> 8) & 255, (b >> 8) & 255));
        int bl = Math.round(MathHelper.lerp(t, a & 255, b & 255));
        return (r << 16) | (g << 8) | bl;
    }

    private static void rrect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double d = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - d * d));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }
}
