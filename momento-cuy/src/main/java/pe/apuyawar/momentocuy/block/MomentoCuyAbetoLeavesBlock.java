package pe.apuyawar.momentocuy.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * Hojas del Abeto Celestial: {@link LeavesBlock} real de vanilla (colisión,
 * capa de render, persistencia/distancia al tronco y descomposición natural
 * iguales a cualquier hoja vanilla — ver {@code MomentoCuyBlocks} para el
 * {@code .ticksRandomly()} que activa esa descomposición), con un
 * {@code randomDisplayTick} propio que añade chispas turquesas/celestes
 * "de luciérnaga" alrededor de las ramas. Usada para las dos variedades
 * (esmeralda y sombría) — las partículas son iguales en ambas; sólo
 * {@code hojas_pino_esmeralda} recibe además luminancia en
 * {@code MomentoCuyBlocks.baseSettings}, para no iluminar todo el bosque.
 *
 * <p><b>Ajuste de visibilidad (ver docs/23, sección "Partículas
 * celestiales")</b>: {@code randomDisplayTick} sólo se invoca para una
 * muestra aleatoria y pequeña de bloques por tick que ya elige el propio
 * motor de Minecraft (no todas las hojas cada tick) — con un árbol de
 * decenas de bloques de hoja, la probabilidad adicional de 1/8 (noche) o
 * 1/20 (día) de la versión anterior, compuesta sobre ese muestreo ya
 * disperso, resultaba en muy pocas partículas visibles de verdad. Se subió
 * la frecuencia y la escala (tamaño visual) de la partícula, y se añadió
 * deriva lateral además de la vertical — "movimiento lateral ligero"
 * pedido en el encargo, antes sólo había deriva en Y. No se duplicó el
 * sistema: sigue siendo el mismo {@code DustParticleEffect} vía
 * {@code randomDisplayTick}, sólo con parámetros distintos.</p>
 *
 * <p><b>Densidad conectada a un control real (corrección "Error 6")</b>:
 * la frecuencia ya no es un número fijo — se divide por
 * {@link MomentoCuyParticleTuning#movingParticleDensityMultiplier()},
 * que `apuyawar-lighting` sincroniza en caliente desde
 * {@code LightingConfig#particleDensityMultiplier} cuando está instalado
 * (si no, se queda en 1.0, el comportamiento normal). Densidad 0
 * desactiva las partículas por completo en vez de calcular una
 * probabilidad absurda.</p> */
public final class MomentoCuyAbetoLeavesBlock extends LeavesBlock {
    private static final DustParticleEffect TURQUESA = new DustParticleEffect(new Vector3f(0.25f, 0.85f, 0.82f), 1.0f);
    private static final DustParticleEffect CELESTE = new DustParticleEffect(new Vector3f(0.42f, 0.70f, 1.0f), 0.9f);

    public MomentoCuyAbetoLeavesBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        float density = MomentoCuyParticleTuning.movingParticleDensityMultiplier();
        if (density <= 0.0f) return;
        int baseChance = world.isNight() ? 4 : 12;
        int chance = Math.max(1, Math.round(baseChance / density));
        if (random.nextInt(chance) != 0) return;
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + random.nextDouble() * 0.6 + 0.1;
        double z = pos.getZ() + random.nextDouble();
        // Deriva lenta en los tres ejes, como una luciérnaga suspendida
        // meciéndose en vez de caer o volar en línea recta.
        double driftX = (random.nextDouble() - 0.5) * 0.006;
        double driftY = (random.nextDouble() - 0.3) * 0.01;
        double driftZ = (random.nextDouble() - 0.5) * 0.006;
        DustParticleEffect effect = random.nextBoolean() ? TURQUESA : CELESTE;
        world.addParticle(effect, x, y, z, driftX, driftY, driftZ);
    }
}
