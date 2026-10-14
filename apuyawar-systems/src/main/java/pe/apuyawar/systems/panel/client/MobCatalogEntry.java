package pe.apuyawar.systems.panel.client;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.text.Text;

/**
 * Una entrada del catálogo de Mobs: identidad real del tipo de criatura, no un
 * ítem. Deliberadamente separado de {@link CatalogEntry} (Objetos sigue usando
 * {@code ItemStack}): una criatura no es un ítem, y forzarla a serlo (como hacía
 * el huevo de generación antes) es exactamente lo que impedía dibujarla en 3D.
 *
 * <p>El tipo acotado a {@code EntityType<? extends MobEntity>} excluye en tiempo
 * de compilación cualquier cosa que no sea una criatura real (proyectiles,
 * vehículos, display entities, el jugador, etc.) — no solo por convención.
 *
 * @param entityType       identidad real de la criatura.
 * @param displayName      nombre mostrado; normalmente {@code entityType.getName()}
 *                         (la traducción vanilla ya existente), nunca inventado.
 * @param category         agrupación informativa (pasiva/neutral/hostil) para
 *                         un futuro orden o filtro del catálogo; no afecta el
 *                         renderizado ni el desbloqueo todavía.
 * @param scaleMultiplier  ajuste manual opcional sobre la escala automática por
 *                         dimensiones (ver {@link MobCellRenderer}); 1.0 = sin
 *                         ajuste. Solo unas pocas criaturas con proporciones
 *                         atípicas lo necesitan.
 * @param verticalOffset   desplazamiente vertical opcional del punto de anclaje
 *                         de la cámara (mismo parámetro {@code yOffset} que usa
 *                         {@code InventoryScreen.drawEntity} para la vista previa
 *                         vanilla del jugador); 0 = centrado automático por altura.
 *
 * <p>No existe todavía un campo de "estado de desbloqueo" individual: igual que
 * en {@link ItemCatalog}, todas las entradas reales se muestran disponibles y el
 * contenido bloqueado se representa aparte, como espacios "?" en
 * {@link MobCatalog#FUTURE_SLOTS} (gestionados por {@link CatalogGrid}, no por
 * esta clase) — no se inventa una segunda noción de bloqueo redundante.
 */
record MobCatalogEntry(
        EntityType<? extends MobEntity> entityType,
        Text displayName,
        Category category,
        float scaleMultiplier,
        float verticalOffset) {

    enum Category { PASSIVE, NEUTRAL, HOSTILE }

    /** Constructor de conveniencia para el caso común: nombre vanilla real, sin
     * ajuste manual de escala/offset. */
    MobCatalogEntry(EntityType<? extends MobEntity> entityType, Category category) {
        this(entityType, entityType.getName(), category, 1f, 0f);
    }

    /** Variante con ajuste manual de escala, para criaturas con proporciones
     * atípicas (ver {@link MobCatalog}). */
    MobCatalogEntry(EntityType<? extends MobEntity> entityType, Category category, float scaleMultiplier) {
        this(entityType, entityType.getName(), category, scaleMultiplier, 0f);
    }
}
