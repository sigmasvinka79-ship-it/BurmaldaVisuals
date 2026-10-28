package com.burmalda.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class Effects {
    private record Circle(double x, double y, double z, long born) {
    }

    private static final int SEG = 64;
    private static final long JUMP_LIFE = 900L;
    private static final long TARGET_LIFE = 3500L;

    private static LivingEntity target;
    private static long lastHit;
    private static boolean wasOnGround = true;
    private static final List<Circle> circles = new ArrayList<>();
    private static final ArrayDeque<Vec3d> trail = new ArrayDeque<>();

    public static void onAttack(Entity e) {
        if (e instanceof LivingEntity le) {
            target = le;
            lastHit = System.currentTimeMillis();
        }
    }

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            circles.clear();
            trail.clear();
            target = null;
            return;
        }
        long now = System.currentTimeMillis();

        boolean onGround = p.isOnGround();
        if (Settings.jumpCircle && wasOnGround && !onGround && p.getVelocity().y > 0.1) {
            circles.add(new Circle(p.getX(), p.getY(), p.getZ(), now));
        }
        wasOnGround = onGround;
        circles.removeIf(c -> now - c.born() > JUMP_LIFE);

        if (Settings.trail) {
            trail.addFirst(new Vec3d(p.getX(), p.getY(), p.getZ()));
            while (trail.size() > 32) trail.removeLast();
        } else {
            trail.clear();
        }

        if (target != null && (target.isRemoved() || now - lastHit > TARGET_LIFE)) target = null;
    }

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        MatrixStack ms = ctx.matrixStack();
        if (p == null || ms == null) return;

        Vec3d cam = ctx.camera().getPos();
        float td = mc.getRenderTickCounter().getTickDelta(false);
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        if (Settings.jumpCircle) {
            for (Circle c : circles) {
                float t = (now - c.born()) / (float) JUMP_LIFE;
                if (t >= 1f) continue;
                float e = 1f - (1f - t) * (1f - t) * (1f - t);
                float rad = 0.25f + e * 1.6f;
                float a = 1f - t;
                float width = 0.05f + 0.3f * (1f - t);
                int col = Settings.colorAt(0f);
                ms.push();
                ms.translate(c.x() - cam.x, c.y() + 0.02 - cam.y, c.z() - cam.z);
                flatRing(ms, rad - width, rad, col, 0f, 0.6f * a);
                flatRing(ms, rad - 0.03f, rad, col, a, a);
                ms.pop();
            }
        }

        LivingEntity tg = target;
        if (Settings.targetEsp && tg != null) {
            long age = now - lastHit;
            float fade = age < 3000L ? 1f : 1f - (age - 3000L) / 500f;
            double x = MathHelper.lerp(td, tg.prevX, tg.getX()) - cam.x;
            double y = MathHelper.lerp(td, tg.prevY, tg.getY()) - cam.y;
            double z = MathHelper.lerp(td, tg.prevZ, tg.getZ()) - cam.z;
            float r = tg.getWidth() * 0.7f + 0.15f;
            float h = tg.getHeight();
            int col = Settings.colorAt(0f);

            ms.push();
            ms.translate(x, y + 0.01, z);
            flatRing(ms, r - 0.12f, r, col, 0f, 0.5f * fade);
            for (int k = 0; k < 2; k++) {
                double ph = (now % 1600L) / 1600.0 * Math.PI * 2 + k * Math.PI;
                float yy = (float) ((Math.sin(ph) * 0.5 + 0.5) * h);
                float dir = Math.cos(ph) >= 0 ? 1f : -1f;
                ms.push();
                ms.translate(0, yy, 0);
                band(ms, r, 0f, -dir * 0.5f, col, 0.85f * fade, 0f);
                flatRing(ms, r - 0.03f, r + 0.01f, col, fade, fade);
                ms.pop();
            }
            ms.pop();
        }

        if (Settings.trail && trail.size() > 1) {
            List<Vec3d> pts = new ArrayList<>();
            pts.add(new Vec3d(
                    MathHelper.lerp(td, p.prevX, p.getX()),
                    MathHelper.lerp(td, p.prevY, p.getY()),
                    MathHelper.lerp(td, p.prevZ, p.getZ())));
            int i = 0;
            for (Vec3d v : trail) {
                if (i++ >= 1) pts.add(v);
            }
            ms.push();
            Matrix4f m = ms.peek().getPositionMatrix();
            BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int n = 0; n < pts.size(); n++) {
                Vec3d v = pts.get(n);
                float f = 1f - n / (float) pts.size();
                int col = Settings.colorAt(n * 0.02f);
                float vx = (float) (v.x - cam.x), vy = (float) (v.y - cam.y), vz = (float) (v.z - cam.z);
                put(b, m, vx, vy + 0.05f, vz, col, 0.65f * f);
                put(b, m, vx, vy + 1.0f, vz, col, 0f);
            }
            BufferRenderer.drawWithGlobalProgram(b.end());
            ms.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Flat ring on the XZ plane around the current origin. */
    private static void flatRing(MatrixStack ms, float inner, float outer, int rgb, float aIn, float aOut) {
        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= SEG; i++) {
            double ang = Math.PI * 2 * i / SEG;
            float cos = (float) Math.cos(ang), sin = (float) Math.sin(ang);
            put(b, m, cos * outer, 0f, sin * outer, rgb, aOut);
            put(b, m, cos * inner, 0f, sin * inner, rgb, aIn);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    /** Vertical cylinder band between two heights with an alpha gradient. */
    private static void band(MatrixStack ms, float radius, float yHead, float yTail, int rgb, float aHead, float aTail) {
        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= SEG; i++) {
            double ang = Math.PI * 2 * i / SEG;
            float cx = (float) Math.cos(ang) * radius, cz = (float) Math.sin(ang) * radius;
            put(b, m, cx, yHead, cz, rgb, aHead);
            put(b, m, cx, yTail, cz, rgb, aTail);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    private static void put(BufferBuilder b, Matrix4f m, float x, float y, float z, int rgb, float a) {
        b.vertex(m, x, y, z).color(
                ((rgb >> 16) & 255) / 255f,
                ((rgb >> 8) & 255) / 255f,
                (rgb & 255) / 255f,
                Math.max(0f, Math.min(1f, a)));
    }

    private Effects() {
    }
}
