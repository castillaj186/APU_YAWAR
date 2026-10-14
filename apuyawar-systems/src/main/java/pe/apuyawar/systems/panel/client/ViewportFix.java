package pe.apuyawar.systems.panel.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;

/**
 * Corrige un viewport de OpenGL que queda mal tras el blur de fondo del menú.
 *
 * Confirmado por desensamblado real del jar de Minecraft 1.21.1:
 * {@code Screen.applyBlur()} llama a {@code GameRenderer.renderBlur()}, que
 * ejecuta el post-proceso de blur (cuyos pases internos, en
 * {@code PostEffectPass.render()}, fijan el viewport al tamaño de SU PROPIO
 * framebuffer de salida vía {@code RenderSystem.viewport(0,0,outW,outH)}, sin
 * restaurarlo al terminar) y luego reenlaza el framebuffer de la ventana con
 * {@code Framebuffer.beginWrite(false)}. El parámetro {@code false} hace que
 * {@code beginWrite} reenlace el FBO pero **no** vuelva a fijar el viewport
 * ({@code Framebuffer.bind(boolean)} solo llama a
 * {@code GlStateManager._viewport(...)} cuando el parámetro es {@code true}).
 *
 * Resultado: justo después de {@code renderBackground()}, el viewport de GL
 * puede seguir siendo el del último paso del shader de blur, no el de la
 * ventana real. Minecraft restaura el viewport correcto una sola vez, al
 * INICIO de cada frame (antes de dibujar el mundo) — nada lo vuelve a hacer
 * después de este blur en concreto, así que cualquier pantalla que dibuje
 * contenido grande inmediatamente después de {@code renderBackground()} lo
 * hace con un viewport potencialmente incorrecto, lo que se percibe como
 * desenfoque/imprecisión en todo lo dibujado hasta el siguiente reinicio de
 * viewport (independientemente del contenido: nuestras texturas o ítems
 * vanilla por igual).
 */
final class ViewportFix {
    /** F9 (en {@link SurvivalPanelScreen}) alterna esto para que el usuario
     * pueda comparar, en la misma sesión y sin recompilar, el panel CON y SIN
     * esta corrección — encendido por defecto (el comportamiento normal y
     * correcto). Apagarlo reproduce a propósito el defecto original, solo
     * como herramienta de verificación. */
    static boolean enabled = true;
    private ViewportFix() {}
    static void restoreAfterBackground(MinecraftClient client) {
        if (!enabled || client == null) return;
        var window = client.getWindow();
        RenderSystem.viewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());
    }
}
