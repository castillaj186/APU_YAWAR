package pe.apuyawar.systems.panel.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Estrategia de renderizado de celda para el catálogo de Mobs: una miniatura 3D
 * real de la criatura (modelo y texturas Vanilla reales, el mismo renderizador
 * que usa el juego), en vez de un ítem/huevo.
 *
 * <h2>API usada (confirmada contra el bytecode real de Minecraft 1.21.1, no supuesta)</h2>
 * {@code InventoryScreen.drawEntity(DrawContext, int x1, int y1, int x2, int y2,
 * int size, float yOffset, float mouseX, float mouseY, LivingEntity)} — la misma
 * ruta con la que Minecraft dibuja al jugador en la pantalla de inventario. Se
 * desensambló su bytecode para confirmar que YA hace, por sí sola, todo lo que
 * pedía el encargo:
 * <ul>
 * <li>Activa {@code context.enableScissor(x1,y1,x2,y2)} al entrar y
 *     {@code disableScissor()} al salir. {@code DrawContext} usa una pila real de
 *     recortes ({@code ScissorStack.push} intersecta con el recorte vigente,
 *     {@code pop} restaura el anterior en vez de desactivarlo del todo) —
 *     confirmado también por bytecode — así que anidarlo dentro del recorte ya
 *     activo del área de contenido del Panel K (en {@code SurvivalPanelScreen})
 *     es seguro: nunca puede dibujar fuera de esa área ni "fugarse" del recorte
 *     exterior.</li>
 * <li>Hace {@code push()}/{@code pop()} de {@code MatrixStack} alrededor de todo
 *     el dibujado.</li>
 * <li>Restaura la iluminación ({@code DiffuseLighting}), el estado de sombras del
 *     {@code EntityRenderDispatcher} y hace {@code context.draw()} para vaciar el
 *     buffer antes de continuar — todo dentro de la propia llamada.</li>
 * <li>Guarda y repone temporalmente {@code bodyYaw}/{@code yaw}/{@code pitch}/
 *     {@code headYaw}/{@code prevHeadYaw} de la entidad: la pose que se ve es
 *     puramente visual y no deja ningún estado permanente en el objeto.</li>
 * </ul>
 * No se usa {@code EntityRenderDispatcher} directamente ni se gestiona el
 * {@code VertexConsumerProvider} a mano: hacerlo habría significado reimplementar
 * con más riesgo algo que esta función ya hace, correcta y nítidamente, desde la
 * propia pantalla de inventario vanilla.
 */
final class MobCellRenderer implements CatalogGrid.CellRenderer<MobCatalogEntry> {
    private static final Logger LOGGER = LoggerFactory.getLogger("apuyawar-panel-k");
    private static final int MARGIN = 3;
    private static final float MIN_SIZE = 4f, MAX_SIZE = 70f;
    /** Calibrado contra el único uso vanilla real conocido de este método: el
     * jugador en la pantalla de inventario se dibuja con {@code size=30} en un
     * recuadro de 49x70 (alto 1.8 bloques) — es decir, al tamaño "ideal" por
     * encuadre (70/1.8≈38.9) se le aplica un 77% para dejar aire arriba/abajo
     * en vez de tocar los bordes exactos de la caja. Se reutiliza el mismo
     * factor aquí para cualquier criatura, calculando el tamaño ideal por
     * ALTURA y por ANCHO reales de {@link EntityType} y usando el menor de los
     * dos — así ninguna criatura se recorta en ningún eje, y las pequeñas
     * (ej. Silverfish, Endermite) aprovechan más la celda en vez de quedar
     * diminutas, en vez de aplicar la misma escala absoluta a todas. */
    private static final float FILL_FACTOR = 0.77f;
    /** Desplazamiento horizontal FIJO (no el mouse real) que pide
     * {@code InventoryScreen.drawEntity} para calcular el ángulo de mirada:
     * {@code yaw = 180° + atan(offsetPx/40) * 40}. Con 36px fijos da un giro de
     * cabeza/cuerpo de 3/4 estable (~29°), igual para toda criatura y toda
     * celda sin importar su tamaño — nunca sigue al cursor ni gira solo. */
    private static final float HEAD_TURN_OFFSET_PX = 36f;

    // Cacheadas por tipo de entidad, no reconstruidas cada frame — ver abajo por
    // qué esta caché es de INSTANCIA (una por CatalogGrid/pantalla), no estática.
    private final Map<EntityType<? extends MobEntity>, LivingEntity> previews = new HashMap<>();
    private final Set<EntityType<? extends MobEntity>> failed = new HashSet<>();

    @Override public void render(DrawContext ctx, TextRenderer tr, MobCatalogEntry entry, int cx, int cy, int cellW, int cellH, boolean hover, boolean selected) {
        if (failed.contains(entry.entityType())) {
            drawFallback(ctx, tr, entry, cx, cy, cellW, cellH);
            return;
        }
        LivingEntity preview = previews.computeIfAbsent(entry.entityType(), this::createPreview);
        if (preview == null) {
            failed.add(entry.entityType());
            drawFallback(ctx, tr, entry, cx, cy, cellW, cellH);
            return;
        }
        try {
            drawPreview(ctx, preview, entry, cx, cy, cellW, cellH);
        } catch (Exception e) {
            // Una criatura que falla no debe afectar a las demás celdas: se
            // recuerda el fallo (no se reintenta cada frame) y se usa el
            // fallback, nunca se deja una excepción sin capturar en medio del
            // render de la cuadrícula.
            LOGGER.warn("[PanelK] No se pudo dibujar la miniatura 3D de {}: {}", EntityType.getId(entry.entityType()), e.toString());
            failed.add(entry.entityType());
            drawFallback(ctx, tr, entry, cx, cy, cellW, cellH);
        }
    }

    /** Solo instancia el objeto Java de la entidad; NUNCA se añade al mundo (no
     * se llama a {@code spawnEntity}/{@code spawn}, solo al factory
     * {@code EntityType.create(World)}), nunca recibe {@code tick()} (sin IA,
     * sin sonidos, sin movimiento, sin paquetes al servidor, sin efectos de
     * juego) y no se comparte entre pantallas: esta caché vive en la instancia
     * de {@code MobCellRenderer}, que a su vez vive en la instancia de
     * {@code CatalogGrid} creada en el constructor de {@code SurvivalPanelScreen}.
     * Cada vez que se presiona K se crea una pantalla (y una caché) nueva, y la
     * anterior queda sin referencias — el recolector de basura la libera solo,
     * sin necesitar un {@code close()}/limpieza explícita. Esto también cubre
     * un cambio de mundo o una desconexión: no hay ninguna caché estática/global
     * que pueda sobrevivir a la pantalla y retener un {@code ClientWorld} viejo. */
    private LivingEntity createPreview(EntityType<? extends MobEntity> type) {
        var client = MinecraftClient.getInstance();
        if (client.world == null) return null;
        try {
            return type.create(client.world);
        } catch (Exception e) {
            LOGGER.warn("[PanelK] No se pudo crear la vista previa de {}: {}", EntityType.getId(type), e.toString());
            return null;
        }
    }

    private void drawPreview(DrawContext ctx, LivingEntity entity, MobCatalogEntry entry, int cx, int cy, int cellW, int cellH) {
        int innerW = Math.max(1, cellW - MARGIN * 2);
        int innerH = Math.max(1, cellH - MARGIN * 2);
        float typeW = Math.max(0.1f, entry.entityType().getWidth());
        float typeH = Math.max(0.1f, entry.entityType().getHeight());
        float sizeByHeight = innerH / typeH;
        float sizeByWidth = innerW / typeW;
        float size = Math.min(sizeByHeight, sizeByWidth) * FILL_FACTOR * entry.scaleMultiplier();
        size = Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));

        int x1 = cx + MARGIN, y1 = cy + MARGIN, x2 = cx + cellW - MARGIN, y2 = cy + cellH - MARGIN;
        float centerX = (x1 + x2) / 2f, centerY = (y1 + y2) / 2f;
        // mouseY = centerY (sin inclinación vertical); mouseX desplazado un fijo
        // HEAD_TURN_OFFSET_PX (no el cursor real) para una pose de 3/4 estable.
        InventoryScreen.drawEntity(ctx, x1, y1, x2, y2, Math.round(size), entry.verticalOffset(),
                centerX - HEAD_TURN_OFFSET_PX, centerY, entity);
    }

    /** No debe confundirse con una celda "?" de contenido futuro (eso lo dibuja
     * {@link CatalogGrid}, el flujo nunca llega aquí para esas celdas): esto es
     * una criatura real que falló al renderizarse en 3D. Se intenta primero su
     * propio huevo de generación (un ítem real de esa misma criatura, no un PNG
     * inventado) si existe; solo si tampoco tiene huevo se dibuja un glifo de
     * advertencia distinto del "?" para no confundir ambos casos. */
    private void drawFallback(DrawContext ctx, TextRenderer tr, MobCatalogEntry entry, int cx, int cy, int cellW, int cellH) {
        SpawnEggItem egg = SpawnEggItem.forEntity(entry.entityType());
        if (egg != null) {
            ctx.getMatrices().push();
            int pad = (cellW - 16 * 2) / 2;
            ctx.getMatrices().translate(cx + pad, cy + pad, 0);
            ctx.getMatrices().scale(2f, 2f, 1f);
            ctx.drawItem(new ItemStack(egg), 0, 0);
            ctx.getMatrices().pop();
        } else {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(cx + cellW / 2f - 3, cy + cellH / 2f - 4, 0);
            ctx.getMatrices().scale(1.6f, 1.6f, 1f);
            ctx.drawText(tr, "!", 0, 0, 0xFFFF5555, false);
            ctx.getMatrices().pop();
        }
    }
}
