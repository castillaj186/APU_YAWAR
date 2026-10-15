package pe.apuyawar.momentocuy.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** Variante de {@link MomentoCuyPlantBlock} con una caja de colisión/selección
 * más ancha y alta, para los arbustos (que ahora se dibujan con 3 planos
 * cruzados en vez de 2, ver {@code template_bush.json}) — más volumen visual
 * necesita una caja algo mayor para que la colisión no se sienta más chica
 * que lo que se ve. */
public final class MomentoCuyBushBlock extends MomentoCuyPlantBlock {
    public static final MapCodec<MomentoCuyBushBlock> BUSH_CODEC = createCodec(MomentoCuyBushBlock::new);
    private static final VoxelShape BUSH_SHAPE = Block.createCuboidShape(2, 0, 2, 14, 13, 14);

    public MomentoCuyBushBlock(AbstractBlock.Settings settings) { super(settings); }

    @Override
    protected MapCodec<? extends PlantBlock> getCodec() { return BUSH_CODEC; }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BUSH_SHAPE;
    }
}
