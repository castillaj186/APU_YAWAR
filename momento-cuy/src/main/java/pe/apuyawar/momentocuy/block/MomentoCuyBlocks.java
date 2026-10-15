package pe.apuyawar.momentocuy.block;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Registro de los 80 bloques de "Momento Cuy" (paquete {@code
 * Momento_Cuy_80_Bloques}), a partir de una tabla de datos — igual que
 * {@code MobCatalog} en el Panel K prioriza una enumeración controlada en vez
 * de adivinar: cada fila es {id, nombre visible, familia de diseño,
 * ¿usa transparencia?}, tomada directamente de {@code INDICE_80_BLOQUES.csv}
 * del paquete de assets, no inventada.
 *
 * <p>Los blockstates/modelos/texturas JSON del paquete se copiaron sin
 * cambios a {@code src/main/resources/assets/momento_cuy/...} (ya usaban el
 * namespace {@code momento_cuy}); esta clase solo decide, por fila:
 * <ul>
 * <li>Qué clase de {@link Block} usar — un {@link Block} simple para la
 *     inmensa mayoría, {@link PillarBlock} para troncos/vigas/pilares
 *     (para que roten por eje como un tronco vanilla), y una clase de
 *     comportamiento dedicada para cada uno de los 7 bloques de minijuegos
 *     ({@code game_*}) — nunca se asume que el JSON ya trae esa lógica.</li>
 * <li>Qué capa de render ({@code BlockRenderLayerMap}) usar según la columna
 *     "usa_transparencia" del CSV: sólido por defecto, cutout para hojas/
 *     brotes/plantas/barandillas, translúcido (alfa real) para vidrios/
 *     cristales/ventanas/el portal — así el canal alfa real de esos 92 PNG
 *     con algún píxel transparente (ver {@code COMPROBACION.txt} del
 *     paquete) se muestra de verdad en vez de verse opaco.</li>
 * </ul>
 */
public final class MomentoCuyBlocks {
    public static final String MOD_ID = "momento_cuy";
    public static final Map<String, Block> BY_ID = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS_BY_ID = new LinkedHashMap<>();

    /** Render de los 7 bloques de minijuegos (ver {@link #SPECIAL}): cada
     * fábrica recibe los {@code Settings} ya preparados por la tabla y
     * devuelve la instancia de comportamiento concreta. */
    private static final Map<String, Function<AbstractBlock.Settings, Block>> SPECIAL = Map.of(
            "plataforma_temporal", TemporaryPlatformBlock::new,
            "bloque_rebotador_rosa", BouncePadBlock::new,
            "baldosa_activadora", TriggerTileBlock::new,
            "bloque_checkpoint", CheckpointBlock::new,
            "bloque_meta", FinishLineBlock::new,
            "portal_momento_cuy", MomentoCuyPortalBlock::new
            // "plataforma_segura" se queda como Block simple: es decorativa,
            // marca una zona segura, no necesita comportamiento propio todavía.
    );

    private record Def(String id, String name, String design, boolean transparent) {
        Layer layer() {
            if (!transparent) return Layer.SOLID;
            return (design.startsWith("glass_") || design.equals("crystal_translucent") || design.startsWith("window_") || design.equals("game_portal"))
                    ? Layer.TRANSLUCENT : Layer.CUTOUT;
        }
        boolean isPillar() { return design.endsWith("_log") || design.endsWith("_beam") || design.equals("quartz_pillar"); }
    }

    // Tabla fuente: INDICE_80_BLOQUES.csv (assets-fuente/bloques/Momento_Cuy_80_Bloques).
    private static final Def[] TABLE = {
            new Def("tierra_fertil", "Tierra fértil", "soil_warm", false),
            new Def("tierra_oscura", "Tierra oscura", "soil_dark", false),
            new Def("tierra_con_raices", "Tierra con raíces", "soil_roots", false),
            new Def("tierra_arcillosa", "Tierra arcillosa", "soil_clay", false),
            new Def("tierra_pedregosa", "Tierra pedregosa", "soil_stones", false),
            new Def("cesped_esmeralda", "Césped esmeralda", "grass_bright", false),
            new Def("cesped_florido", "Césped florido", "grass_flowers", false),
            new Def("cesped_musgoso", "Césped musgoso", "grass_moss", false),
            new Def("sendero_de_tierra", "Sendero de tierra", "soil_path", false),
            new Def("suelo_de_petalos", "Suelo de pétalos", "soil_petals", false),
            new Def("roca_azul_cielo", "Roca azul cielo", "rock_sky", false),
            new Def("roca_azul_profunda", "Roca azul profunda", "rock_deep", false),
            new Def("piedra_nube", "Piedra nube", "rock_cloud", false),
            new Def("piedra_estratificada", "Piedra estratificada", "rock_strata", false),
            new Def("roca_vetas_turquesas", "Roca con vetas turquesas", "rock_ore", false),
            new Def("roca_musgosa", "Roca musgosa", "rock_moss", false),
            new Def("roca_fracturada", "Roca fracturada", "rock_crack", false),
            new Def("piedra_pulida_azulada", "Piedra pulida azulada", "rock_polished", false),
            new Def("adoquines_acantilado", "Adoquines de acantilado", "rock_cobble", false),
            new Def("piedra_runica_antigua", "Piedra rúnica antigua", "rock_rune", false),
            new Def("tronco_cerezo_celestial", "Tronco de cerezo celestial", "cherry_log", false),
            new Def("madera_cerezo_completa", "Madera de cerezo completa", "cherry_wood", false),
            new Def("tronco_cerezo_pelado", "Tronco de cerezo pelado", "cherry_stripped", false),
            new Def("tablones_cerezo", "Tablones de cerezo", "cherry_planks", false),
            new Def("mosaico_cerezo", "Mosaico de cerezo", "cherry_mosaic", false),
            new Def("viga_cerezo", "Viga de cerezo", "cherry_beam", false),
            new Def("hojas_cerezo_rosa_claro", "Hojas de cerezo rosa claro", "leaf_pinklight", true),
            new Def("hojas_cerezo_rosa_intenso", "Hojas de cerezo rosa intenso", "leaf_pink", true),
            new Def("hojas_cerezo_florecido", "Hojas de cerezo florecido", "leaf_blossom", true),
            new Def("brote_cerezo", "Brote de cerezo", "sapling_cherry", true),
            new Def("tronco_abeto_celestial", "Tronco de abeto celestial", "spruce_log", false),
            new Def("madera_abeto_completa", "Madera de abeto completa", "spruce_wood", false),
            new Def("tronco_abeto_pelado", "Tronco de abeto pelado", "spruce_stripped", false),
            new Def("tablones_abeto", "Tablones de abeto", "spruce_planks", false),
            new Def("vigas_reforzadas_abeto", "Vigas reforzadas de abeto", "spruce_beam", false),
            new Def("hojas_pino_esmeralda", "Hojas de pino esmeralda", "leaf_pine", true),
            new Def("hojas_pino_sombrio", "Hojas de pino sombrío", "leaf_darkpine", true),
            new Def("brote_abeto_celestial", "Brote de abeto celestial", "sapling_spruce", true),
            new Def("cuarzo_lavanda_natural", "Cuarzo lavanda natural", "quartz_raw", false),
            new Def("cuarzo_lavanda_liso", "Cuarzo lavanda liso", "quartz_smooth", false),
            new Def("cuarzo_lavanda_pulido", "Cuarzo lavanda pulido", "quartz_polished", false),
            new Def("ladrillos_cuarzo_lavanda", "Ladrillos de cuarzo lavanda", "quartz_brick", false),
            new Def("ladrillos_cuarzo_agrietados", "Ladrillos de cuarzo agrietados", "quartz_cracked", false),
            new Def("pilar_cuarzo_lavanda", "Pilar de cuarzo lavanda", "quartz_pillar", false),
            new Def("cuarzo_cincelado_cuy", "Cuarzo cincelado del Cuy", "quartz_chiseled", false),
            new Def("baldosa_ornamental_lavanda", "Baldosa ornamental lavanda", "quartz_tile", false),
            new Def("cuarzo_rosa_perlado", "Cuarzo rosa perlado", "quartz_pink", false),
            new Def("cuarzo_nube", "Cuarzo nube", "quartz_cloud", false),
            new Def("mineral_aguamarina", "Mineral de aguamarina", "crystal_ore", false),
            new Def("bloque_aguamarina", "Bloque de aguamarina", "crystal_aqua", false),
            new Def("cristal_translucido_celestial", "Cristal translúcido celestial", "crystal_translucent", true),
            new Def("cristal_prismatico_rosa", "Cristal prismático rosa", "crystal_pink", false),
            new Def("lampara_cristal_celestial", "Lámpara de cristal celestial", "crystal_lamp", false),
            new Def("vidrio_transparente_puro", "Vidrio transparente puro", "glass_clear", true),
            new Def("vidrio_rosado", "Vidrio rosado", "glass_pink", true),
            new Def("vitral_encantado", "Vitral encantado", "glass_stained", true),
            new Def("bloque_arco_celestial", "Bloque de arco celestial", "build_arch", false),
            new Def("ladrillos_piedra_azul", "Ladrillos de piedra azul", "build_bluebrick", false),
            new Def("adoquines_nube_clara", "Adoquines de nube clara", "build_cloudcobble", false),
            new Def("tablones_puente_envejecido", "Tablones de puente envejecido", "build_oldplanks", false),
            new Def("viga_puente_reforzada", "Viga de puente reforzada", "build_bridgebeam", false),
            new Def("bloque_cuerda_trenzada", "Bloque de cuerda trenzada", "build_rope", false),
            new Def("barandilla_madera_celestial", "Barandilla de madera celestial", "build_woodrailing", true),
            new Def("barandilla_piedra_ornamental", "Barandilla de piedra ornamental", "build_stonerailling", true),
            new Def("ventana_cerezo", "Ventana de cerezo", "window_cherry", true),
            new Def("ventana_cuarzo_lavanda", "Ventana de cuarzo lavanda", "window_lavender", true),
            new Def("arbusto_esmeralda", "Arbusto esmeralda", "plant_bush", true),
            new Def("arbusto_florecido", "Arbusto florecido", "plant_flowerbush", true),
            new Def("seto_jardin", "Seto de jardín", "plant_hedge", true),
            new Def("enredadera_celestial", "Enredadera celestial", "plant_vine", true),
            new Def("musgo_suspendido", "Musgo suspendido", "plant_hangingmoss", true),
            new Def("flor_estrella_azul", "Flor estrella azul", "plant_blueflower", true),
            new Def("flor_solar_dorada", "Flor solar dorada", "plant_goldflower", true),
            new Def("plataforma_segura", "Plataforma segura", "game_safe", false),
            new Def("plataforma_temporal", "Plataforma temporal", "game_temporary", false),
            new Def("bloque_rebotador_rosa", "Bloque rebotador rosa", "game_bounce", false),
            new Def("baldosa_activadora", "Baldosa activadora", "game_trigger", false),
            new Def("bloque_checkpoint", "Bloque de checkpoint", "game_checkpoint", false),
            new Def("bloque_meta", "Bloque de meta", "game_finish", false),
            new Def("portal_momento_cuy", "Portal del Momento Cuy", "game_portal", true),
    };

    private MomentoCuyBlocks() {}

    public static void initialize() {
        for (Def def : TABLE) register(def);
        Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "bloques"), FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.momento_cuy.bloques"))
                .icon(() -> new ItemStack(BY_ID.get("cuarzo_cincelado_cuy")))
                .entries((context, entries) -> ITEMS_BY_ID.values().forEach(entries::add))
                .build());
    }

    private static void register(Def def) {
        AbstractBlock.Settings settings = baseSettings(def);
        Block block = newBlock(def, settings);
        Identifier id = Identifier.of(MOD_ID, def.id());
        Registry.register(Registries.BLOCK, id, block);
        Item item = Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
        BY_ID.put(def.id(), block);
        ITEMS_BY_ID.put(def.id(), item);
    }

    /** Qué clase de {@link Block} usar por fila — ver
     * {@code docs/21_MOMENTO_CUY_GEOMETRIA_BLOQUES.md} para el porqué de cada
     * elección geométrica. Brotes/flores: {@link MomentoCuyPlantBlock} (no
     * sólido, sin mecánica de crecimiento inventada). Arbustos: la variante
     * con más volumen {@link MomentoCuyBushBlock}. Enredadera/musgo colgante:
     * el {@link VineBlock} real de vanilla, sin tocar su configuración de
     * ticks aleatorios (queda apagada por defecto en {@code Settings}, así
     * que nunca se expande sola — puramente decorativo). Barandilla de
     * madera: {@link FenceBlock} real (conexiones automáticas). Barandilla
     * de piedra: {@link WallBlock} real (postes/conexiones bajo/alto). El
     * seto de jardín se queda como {@link Block} normal a propósito: un seto
     * recortado es, por definición, una pared sólida de vegetación, no una
     * planta suelta — ver la documentación para el resto de bloques
     * revisados y no cambiados. */
    private static Block newBlock(Def def, AbstractBlock.Settings settings) {
        if (SPECIAL.containsKey(def.id())) return SPECIAL.get(def.id()).apply(settings);
        if (def.isPillar()) return new PillarBlock(settings);
        return switch (def.design()) {
            case "sapling_cherry", "sapling_spruce", "plant_blueflower", "plant_goldflower" -> new MomentoCuyPlantBlock(settings);
            case "plant_bush", "plant_flowerbush" -> new MomentoCuyBushBlock(settings);
            case "plant_vine", "plant_hangingmoss" -> new VineBlock(settings);
            case "build_woodrailing" -> new FenceBlock(settings);
            case "build_stonerailling" -> new WallBlock(settings);
            default -> new Block(settings);
        };
    }

    private static AbstractBlock.Settings baseSettings(Def def) {
        AbstractBlock.Settings settings = AbstractBlock.Settings.create()
                .mapColor(mapColorFor(def.design()))
                .strength(strengthFor(def.design()))
                .sounds(soundsFor(def.design()));
        if (def.layer() != Layer.SOLID) settings = settings.nonOpaque();
        // Brotes, flores, arbustos y enredaderas/musgo: sin colisión, como
        // cualquier planta vanilla (la caja de MomentoCuyPlantBlock/
        // MomentoCuyBushBlock solo sirve para el contorno de selección). El
        // seto de jardín NO entra aquí a propósito: es una pared sólida de
        // vegetación recortada, no una planta suelta -- conserva colisión.
        if (isNonSolidPlant(def.design())) settings = settings.noCollision();
        return settings;
    }

    private static boolean isNonSolidPlant(String design) {
        return design.startsWith("sapling_") || design.equals("plant_blueflower") || design.equals("plant_goldflower")
                || design.equals("plant_bush") || design.equals("plant_flowerbush")
                || design.equals("plant_vine") || design.equals("plant_hangingmoss");
    }

    /** Dureza razonable por familia (no inventada por bloque, solo agrupada):
     * tierra/plantas blandas, piedra/cuarzo/construcción firmes, cristal algo
     * más resistente, los bloques de minijuego firmes para que no estorben el
     * ritmo de una partida. */
    private static float strengthFor(String design) {
        if (design.startsWith("soil_") || design.startsWith("grass_")) return 0.6f;
        if (design.startsWith("leaf_") || design.startsWith("sapling_") || design.startsWith("plant_")) return 0.3f;
        if (design.startsWith("glass_") || design.startsWith("window_") || design.equals("crystal_translucent")) return 0.4f;
        if (design.startsWith("cherry_") || design.startsWith("spruce_") || design.startsWith("build_")) return 2.0f;
        if (design.startsWith("crystal_")) return 2.5f;
        if (design.startsWith("game_")) return 3.0f;
        return 1.5f;
    }

    private static net.minecraft.sound.BlockSoundGroup soundsFor(String design) {
        if (design.startsWith("soil_") || design.startsWith("grass_")) return net.minecraft.sound.BlockSoundGroup.GRASS;
        if (design.startsWith("leaf_") || design.startsWith("sapling_") || design.startsWith("plant_")) return net.minecraft.sound.BlockSoundGroup.AZALEA_LEAVES;
        if (design.startsWith("glass_") || design.startsWith("window_") || design.equals("crystal_translucent")) return net.minecraft.sound.BlockSoundGroup.GLASS;
        if (design.startsWith("cherry_") || design.startsWith("spruce_") || design.startsWith("build_")) return net.minecraft.sound.BlockSoundGroup.WOOD;
        if (design.startsWith("crystal_")) return net.minecraft.sound.BlockSoundGroup.AMETHYST_BLOCK;
        return net.minecraft.sound.BlockSoundGroup.STONE;
    }

    private static MapColor mapColorFor(String design) {
        if (design.startsWith("soil_")) return MapColor.DIRT_BROWN;
        if (design.startsWith("grass_")) return MapColor.GREEN;
        if (design.startsWith("rock_")) return MapColor.STONE_GRAY;
        if (design.startsWith("cherry_") || design.startsWith("spruce_") || design.startsWith("build_")) return MapColor.TERRACOTTA_WHITE;
        if (design.startsWith("leaf_")) return MapColor.PALE_GREEN;
        if (design.startsWith("sapling_")) return MapColor.PALE_GREEN;
        if (design.startsWith("quartz_")) return MapColor.PALE_PURPLE;
        if (design.startsWith("crystal_")) return MapColor.DIAMOND_BLUE;
        if (design.startsWith("glass_") || design.startsWith("window_")) return MapColor.LIGHT_BLUE;
        if (design.startsWith("plant_")) return MapColor.DARK_GREEN;
        if (design.startsWith("game_")) return MapColor.PINK;
        return MapColor.STONE_GRAY;
    }

    /** Expone, para el registro de capas de render del CLIENTE
     * ({@code MomentoCuyClient}), qué bloques necesitan cutout/translúcido —
     * datos puros (sin referenciar {@code net.minecraft.client...}), para que
     * esta clase siga siendo segura de cargar en un servidor dedicado. */
    public static Map<Block, Layer> nonSolidBlocks() {
        Map<Block, Layer> result = new LinkedHashMap<>();
        for (Def def : TABLE) {
            if (def.layer() != Layer.SOLID) result.put(BY_ID.get(def.id()), def.layer());
        }
        return result;
    }

    public enum Layer { SOLID, CUTOUT, TRANSLUCENT }
}
