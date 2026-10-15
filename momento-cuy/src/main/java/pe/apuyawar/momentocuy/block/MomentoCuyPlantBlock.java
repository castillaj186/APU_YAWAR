package pe.apuyawar.momentocuy.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Base para los brotes y flores de Momento Cuy (ver
 * {@code docs/21_MOMENTO_CUY_GEOMETRIA_BLOQUES.md}): una planta real no
 * sólida en vez del cubo completo que usaban antes. Deliberadamente NO es
 * {@code SaplingBlock} (que exige un {@code SaplingGenerator} para hacer
 * crecer un árbol adulto) ni {@code FlowerBlock} (que exige un efecto de
 * guiso sospechoso) — ninguna de las dos cosas se pidió, y esta corrección
 * es solo de geometría/colisión, no de mecánicas nuevas. Ver
 * {@link MomentoCuyBushBlock} para la variante con más volumen (arbustos).
 */
public class MomentoCuyPlantBlock extends PlantBlock {
    public static final MapCodec<MomentoCuyPlantBlock> CODEC = createCodec(MomentoCuyPlantBlock::new);

    /** Caja pequeña, como una flor/brote vanilla (no bloquea casi el paso). */
    protected static final VoxelShape SHAPE = Block.createCuboidShape(3, 0, 3, 13, 10, 13);

    public MomentoCuyPlantBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends PlantBlock> getCodec() { return CODEC; }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    /** Permisivo a propósito: esto es una biblioteca de construcción para que
     * el equipo levante jardines donde quiera, no una simulación de
     * agricultura con restricciones de tipo de suelo inventadas — basta con
     * que haya un bloque sólido debajo. */
    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isSideSolidFullSquare(world, pos, Direction.UP);
    }
}
