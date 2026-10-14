package pe.apuyawar.momentocuy.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Registro del Cuy Jardinero. Un solo huevo de generación para pruebas/
 * colocación por un operador; la dimensión en sí ya garantiza que exista al
 * menos un Cuy en la plataforma de llegada (ver {@code MomentoCuyLobby}) sin
 * depender de que nadie use el huevo. */
public final class MomentoCuyEntities {
    public static final EntityType<CuyJardineroEntity> CUY_JARDINERO = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("momento_cuy", "cuy_jardinero"),
            EntityType.Builder.create(CuyJardineroEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.9F, 0.8F)
                    .maxTrackingRange(8)
                    .build(Identifier.of("momento_cuy", "cuy_jardinero").toString()));

    public static final Item CUY_JARDINERO_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            Identifier.of("momento_cuy", "cuy_jardinero_spawn_egg"),
            new SpawnEggItem(CUY_JARDINERO, 0xD9A066, 0x6B8E4E, new Item.Settings()));

    private MomentoCuyEntities() {}

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(CUY_JARDINERO, CuyJardineroEntity.createAttributes());
    }
}
