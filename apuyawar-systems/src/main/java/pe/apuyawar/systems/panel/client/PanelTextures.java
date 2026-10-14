package pe.apuyawar.systems.panel.client;

import net.minecraft.util.Identifier;

/**
 * Identificadores de las texturas del Panel K; única fuente de verdad de rutas/tamaños.
 *
 * Los íconos NO son los PNG de {@code assets-fuente} tal cual (esos miden 1254x1254 y
 * se mantienen intactos ahí, sin tocarlos). Son copias optimizadas preparadas para el
 * tamaño real al que se ven en el juego: reducidas con interpolación bicúbica en
 * espacio de alfa premultiplicado (evita halos oscuros en los bordes transparentes) y
 * con un realce de nitidez (unsharp mask) para compensar el antialiasing suave que ya
 * traían los PNG originales — eso, no el filtrado de Minecraft, era la causa real del
 * desenfoque persistente (confirmado midiendo la transición de alfa real: ~4-5 píxeles
 * de degradado en los bordes, no un salto duro de 1 píxel). {@code panel.png} se dejó
 * a su resolución nativa 1672x941 con el mismo realce, sin reducir — reducirlo
 * arriesgaba forzar una ampliación de vuelta en pantallas grandes con GUI Scale bajo,
 * lo que habría reintroducido el desenfoque para esos usuarios.
 */
final class PanelTextures {
    static final Identifier PANEL = id("panel_k/panel");
    static final Identifier ICON_MOBS = id("panel_k/icon_mobs");
    static final Identifier ICON_ITEMS = id("panel_k/icon_items");
    static final Identifier ICON_MISSIONS = id("panel_k/icon_missions");
    static final Identifier ICON_SEARCH = id("panel_k/icon_search");
    static final int PANEL_W = 1672, PANEL_H = 941;
    static final int ICON_SIZE = 512;
    private PanelTextures() {}
    private static Identifier id(String path) { return Identifier.of("apuyawar_systems", "textures/gui/" + path + ".png"); }
}
