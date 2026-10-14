package pe.apuyawar.systems.panel.client;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Catálogo de "Cambios y Mobs": criaturas reales de Vanilla, representadas por su
 * identidad real de entidad ({@link MobCatalogEntry}), no por su huevo de
 * generación — un huevo es solo un ítem para generar una entidad, no la
 * definición de qué criaturas existen (algunas, como el Warden, el Golem de
 * Nieve o el Golem de Hierro, no tienen huevo y sin embargo son criaturas Vanilla
 * reales que sí deben aparecer aquí).
 *
 * <p>Enumeración controlada y explícita en vez de filtrar el registro de
 * entidades genéricamente: {@code SpawnGroup} por sí solo no basta (el Warden,
 * los Golems y el Soporte de Armadura comparten {@code MISC} con entidades que
 * NO son criaturas), así que cada entrada se lista a mano — reproducible, sin
 * inventar ninguna criatura, y cada constante es una referencia real del
 * registro de {@link EntityType} (un typo aquí es un error de compilación, no
 * una sorpresa en tiempo de ejecución).
 *
 * <p>Excluidos explícitamente (jefes, no "mobs" de bestiario): {@link EntityType#ENDER_DRAGON},
 * {@link EntityType#WITHER}. Excluidos por no ser criaturas (proyectiles, vehículos,
 * entidades de decoración/visualización, el propio jugador): todo lo que no
 * aparece en la lista de abajo.
 *
 * <p>El contenido futuro (de este equipo o de colaboradores) se representa con
 * espacios "?" vacíos — ver {@link #FUTURE_SLOTS} — sin inventar ningún registro
 * ni desbloquear automáticamente nada. Para añadir una criatura nueva de un mod
 * propio más adelante, basta con agregar una línea a {@link #build()}; no hace
 * falta tocar {@link MobCatalogEntry}, {@link MobCellRenderer} ni {@link CatalogGrid}.
 */
final class MobCatalog {
    /** Cuántos espacios "?" de contenido futuro mostrar al final de la cuadrícula. */
    static final int FUTURE_SLOTS = 16;
    static final List<MobCatalogEntry> ENTRIES = Collections.unmodifiableList(build());
    private MobCatalog() {}

    private static List<MobCatalogEntry> build() {
        List<MobCatalogEntry> list = new ArrayList<>();

        // --- Pasivas / amistosas ---
        passive(list, EntityType.ALLAY, EntityType.ARMADILLO, EntityType.AXOLOTL, EntityType.BAT,
                EntityType.CAMEL, EntityType.CAT, EntityType.CHICKEN, EntityType.COD, EntityType.COW,
                EntityType.DONKEY, EntityType.FROG, EntityType.GLOW_SQUID, EntityType.HORSE,
                EntityType.MOOSHROOM, EntityType.MULE, EntityType.OCELOT, EntityType.PANDA,
                EntityType.PARROT, EntityType.PIG, EntityType.PUFFERFISH, EntityType.RABBIT,
                EntityType.SALMON, EntityType.SHEEP, EntityType.SKELETON_HORSE, EntityType.SNIFFER,
                EntityType.SNOW_GOLEM, EntityType.SQUID, EntityType.STRIDER, EntityType.TADPOLE,
                EntityType.TRADER_LLAMA, EntityType.TROPICAL_FISH, EntityType.TURTLE,
                EntityType.VILLAGER, EntityType.WANDERING_TRADER);

        // --- Neutrales (pacíficas salvo que se las provoque) ---
        neutral(list, EntityType.BEE, EntityType.DOLPHIN, EntityType.ENDERMAN, EntityType.FOX,
                EntityType.GOAT, EntityType.IRON_GOLEM, EntityType.LLAMA, EntityType.PIGLIN,
                EntityType.POLAR_BEAR, EntityType.WOLF);

        // --- Hostiles ---
        hostile(list, EntityType.BLAZE, EntityType.BOGGED, EntityType.BREEZE, EntityType.CAVE_SPIDER,
                EntityType.CREEPER, EntityType.DROWNED, EntityType.ELDER_GUARDIAN, EntityType.ENDERMITE,
                EntityType.EVOKER, EntityType.GHAST, EntityType.GIANT, EntityType.GUARDIAN,
                EntityType.HOGLIN, EntityType.HUSK, EntityType.ILLUSIONER, EntityType.MAGMA_CUBE,
                EntityType.PHANTOM, EntityType.PIGLIN_BRUTE, EntityType.PILLAGER, EntityType.RAVAGER,
                EntityType.SHULKER, EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SLIME,
                EntityType.SPIDER, EntityType.STRAY, EntityType.VEX, EntityType.VINDICATOR,
                EntityType.WARDEN, EntityType.WITCH, EntityType.WITHER_SKELETON, EntityType.ZOGLIN,
                EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN);

        list.sort((a, b) -> a.displayName().getString().compareToIgnoreCase(b.displayName().getString()));
        return list;
    }

    @SafeVarargs
    private static void passive(List<MobCatalogEntry> list, EntityType<? extends MobEntity>... types) {
        for (var type : types) list.add(new MobCatalogEntry(type, MobCatalogEntry.Category.PASSIVE));
    }
    @SafeVarargs
    private static void neutral(List<MobCatalogEntry> list, EntityType<? extends MobEntity>... types) {
        for (var type : types) list.add(new MobCatalogEntry(type, MobCatalogEntry.Category.NEUTRAL));
    }
    @SafeVarargs
    private static void hostile(List<MobCatalogEntry> list, EntityType<? extends MobEntity>... types) {
        for (var type : types) list.add(new MobCatalogEntry(type, MobCatalogEntry.Category.HOSTILE));
    }
}
