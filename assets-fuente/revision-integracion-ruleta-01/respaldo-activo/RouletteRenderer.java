package pe.apuyawar.systems.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import pe.apuyawar.systems.roulette.RoulettePalette;
import pe.apuyawar.systems.roulette.RouletteTimeline;

/**
 * Renderizador de la ruleta APU YAWAR.
 *
 * - El disco (8 sectores, aro y medallon) gira como una sola unidad.
 * - El puntero se dibuja FUERA de la matriz de rotacion: queda fijo.
 * - Los destellos se dibujan antes del puntero y NO cubren el disco.
 * - El ganador procede del estado comunicado por el servidor.
 *
 * Este archivo conserva el protocolo y la coreografia de la version 0.1.0.
 */
public final class RouletteRenderer {
    private static final int TEXTURE_SIZE = 512;
    private static final int TEXTURE_CENTER = TEXTURE_SIZE / 2;
    // Diámetro del aro activo, sin los márgenes transparentes del lienzo.
    private static final float WHEEL_DIAMETER = 386f;
    private static final float SCREEN_HEIGHT_FRACTION = 0.42f;

    private static final Identifier[] SECTORS = new Identifier[8];
    private static final Identifier RING = texture("ring");
    private static final Identifier CENTER = texture("medallion");
    private static final Identifier POINTER = texture("pointer");
    private static final Identifier FLASH = texture("flash");
    private static final Identifier GLOW = texture("glow");

    static {
        for (int i = 0; i < SECTORS.length; i++) {
            SECTORS[i] = texture("sector_" + RoulettePalette.NAMES[i]);
        }
    }

    private RouletteRenderer() { }

    private static Identifier texture(String name) {
        return Identifier.of("apuyawar_systems", "textures/gui/roulette/" + name + ".png");
    }

    private static void drawTexture(DrawContext context, Identifier id) {
        context.drawTexture(id, -TEXTURE_CENTER, -TEXTURE_CENTER, 0, 0,
                TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /** Cada capa usa el mismo pivote y restaura su transformación al terminar. */
    private static void drawLayer(DrawContext context, Identifier id, float rotation,
                                  float layerScale, int rgb, float opacity) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
            matrices.scale(layerScale, layerScale, 1f);
            drawTinted(context, id, rgb, opacity);
        } finally {
            matrices.pop();
        }
    }

    /** Multiplica color y opacidad; el alfa real del PNG se conserva. */
    private static void drawTinted(DrawContext context, Identifier id, int rgb, float opacity) {
        float a = Math.max(0f, Math.min(1f, opacity));
        if (a <= 0f) return;

        RenderSystem.setShaderColor(
                ((rgb >> 16) & 0xFF) / 255f,
                ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f,
                a
        );
        try {
            drawTexture(context, id);
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    public static void render(DrawContext context, float tickDelta) {
        if (!RouletteClientState.isRunning()) return;

        final float tick = RouletteClientState.time(tickDelta);
        final int winner = RouletteClientState.winner();
        if (winner < 0 || winner >= SECTORS.length) return;

        final float opacity = (float) RouletteTimeline.alpha(tick);
        final float scale = (float) RouletteTimeline.scale(tick);
        if (opacity <= 0f || scale <= 0f) return;

        final float shake = (float) RouletteTimeline.shake(tick);
        final float dx = shake * (float) Math.sin(tick * 5.3f);
        final float dy = shake * (float) Math.cos(tick * 7.7f);

        final int centerX = context.getScaledWindowWidth() / 2;
        final int centerY = context.getScaledWindowHeight() / 2;
        // Tamaño proporcional a la pantalla, independiente de la escala de GUI.
        // El lienzo incluye espacio para el puntero y los efectos exteriores.
        final float desiredSide = context.getScaledWindowHeight()
                * SCREEN_HEIGHT_FRACTION * TEXTURE_SIZE / WHEEL_DIAMETER;
        final float availableSide = Math.max(1f,
                Math.min(context.getScaledWindowWidth(), context.getScaledWindowHeight()) - 18f);
        final float side = Math.min(desiredSide, availableSide);

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(centerX + dx, centerY + dy, 150);
            float guiScale = side / (float) TEXTURE_SIZE * scale;
            matrices.scale(guiScale, guiScale, 1f);

            // Halo ambiental sutil por detras, nunca un circulo opaco.
            drawLayer(context, GLOW, 0f, 1f, 0xF6BE59, 0.20f * opacity);

            // Capas independientes; comparten el ángulo actual sin heredar rotación.
            float diskRotation = (float) RouletteTimeline.rotationDegrees(tick, winner);
            int visible = Math.min(SECTORS.length, Math.max(0, RouletteTimeline.visibleSectors(tick)));
            for (int i = 0; i < visible; i++) {
                drawLayer(context, SECTORS[i], diskRotation, 1f, 0xFFFFFF, opacity);
            }
            drawLayer(context, RING, diskRotation, 1f, 0xFFFFFF, opacity);
            drawLayer(context, CENTER, diskRotation, 1f, 0xFFFFFF, opacity);

            // La confirmacion ilumina el resultado pero deja ver los simbolos.
            if (tick >= 290f && tick < 338f) {
                float shimmer = (float) (0.5 + 0.5 * Math.sin(tick * 0.68));
                drawLayer(context, GLOW, 0f, 1f, RoulettePalette.RGB[winner],
                        (0.055f + 0.065f * shimmer) * opacity);
            }

            // 'flash.png' tiene el CENTRO TRANSPARENTE; no borra el disco.
            float flash = (float) RouletteTimeline.flash(tick);
            if (flash > 0f) {
                int flashColor = tick < 40f ? 0xFFFFFF : RoulettePalette.RGB[winner];
                float strength = tick < 40f ? 0.45f : 0.65f;
                drawLayer(context, FLASH, 0f, 1f, flashColor, flash * strength * opacity);
            }

            // Al dibujarlo al final y sin rotacion, el puntero permanece nitido.
            drawLayer(context, POINTER, 0f, 1f, 0xFFFFFF, opacity);
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            matrices.pop();
        }
    }
}
