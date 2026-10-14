package pe.apuyawar.systems.client.judgment;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import pe.apuyawar.systems.judgment.IntiPresentationTimeline;

/** Uses complete transparent PNG frames; no atlas UV sampling is involved. */
public final class IntiRenderer {
    private static final int FRAME_SIZE = 512;
    private static final Identifier[] FRAMES = new Identifier[IntiPresentationTimeline.FRAME_COUNT];
    static {
        for (int frame = 0; frame < FRAMES.length; frame++) {
            FRAMES[frame] = Identifier.of("apuyawar_systems", "textures/gui/inti/frames/frame_%03d.png".formatted(frame + 1));
        }
    }
    private IntiRenderer() {}
    public static void render(DrawContext context, float tickDelta) {
        if (!IntiClientState.isRunning()) return;
        int frame = IntiClientState.frame(tickDelta);
        float progress = IntiClientState.time(tickDelta) / IntiPresentationTimeline.DURATION_TICKS;
        float appear = Math.min(1f, progress * 8f);
        float vanish = progress > .93f ? 1f - (progress - .93f) / .07f : 1f;
        float alpha = Math.max(0f, Math.min(1f, appear * vanish));
        // The roulette is about 50% of screen height; Inti is rendered at 1.10x that diameter.
        int side = Math.min((int)(context.getScaledWindowHeight() * .55f), (int)(context.getScaledWindowWidth() * .62f));
        int x = (context.getScaledWindowWidth() - side) / 2;
        int y = (context.getScaledWindowHeight() - side) / 2;
        // The aura reuses the alpha mask of the complete frame, never a solid rectangle or disc.
        if (progress < .34f || progress > .79f) {
            int aura = side + Math.max(6, side / 30);
            context.setShaderColor(1f, .18f, .12f, alpha * .14f);
            drawFullFrame(context, frame, x - (aura - side) / 2, y - (aura - side) / 2, aura);
        }
        context.setShaderColor(1f, 1f, 1f, alpha);
        drawFullFrame(context, frame, x, y, side);
        context.setShaderColor(1f, 1f, 1f, 1f);
    }
    private static void drawFullFrame(DrawContext context, int frame, int x, int y, int side) {
        // 1.21.1 overload: destination x/y/width/height, then source u/v/width/height and full texture size.
        // Keeping both sizes explicit guarantees that the complete 512 x 512 PNG is sampled even when side < 512.
        context.drawTexture(FRAMES[frame], x, y, side, side,
            0.0f, 0.0f, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);
    }
}
