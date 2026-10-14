package pe.apuyawar.momentocuy.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.Goal.Control;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.EnumSet;

/** Adaptado sin cambios de comportamiento del {@code CuyHarvestGoal} del mod
 * antiguo (ver {@code CuyJardineroEntity} para el proceso de recuperación). */
public final class CuyHarvestGoal extends Goal {
    private final CuyJardineroEntity cuy;
    private BlockPos target;
    private BlockPos standing;
    private int nextAttempt;
    private int deadline;
    private boolean running;

    public CuyHarvestGoal(CuyJardineroEntity cuy) {
        this.cuy = cuy;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private double distance(BlockPos pos) {
        return this.cuy.squaredDistanceTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    @Override
    public boolean canStart() {
        if (this.cuy.age >= this.nextAttempt && !this.cuy.isCuyBusy() && !this.cuy.getCuyFleeing()
                && this.cuy.isOnGround() && this.cuy.getCustomer() == null && this.cuy.getCuyFarm().isWorkingTime()) {
            this.nextAttempt = this.cuy.age + 80;
            BlockPos station = this.cuy.getCuyFarm().getStation();
            if (station == null) return false;
            this.target = null;
            this.standing = null;
            double best = Double.MAX_VALUE;
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    for (int y = -2; y <= 2; y++) {
                        BlockPos plant = station.add(x, y, z);
                        BlockState state = this.cuy.getWorld().getBlockState(plant);
                        Block block = state.getBlock();
                        if (block instanceof CropBlock crop) {
                            if (crop.isMature(state)
                                    && (state.isOf(Blocks.WHEAT) || state.isOf(Blocks.BEETROOTS) || state.isOf(Blocks.CARROTS) || state.isOf(Blocks.POTATOES))
                                    && this.cuy.getWorld().getBlockState(plant.down()).isOf(Blocks.FARMLAND)) {
                                for (Direction dir : Direction.Type.HORIZONTAL) {
                                    BlockPos place = plant.offset(dir);
                                    BlockPos floor = place.down();
                                    BlockState ground = this.cuy.getWorld().getBlockState(floor);
                                    if (this.cuy.getWorld().isAir(place) && this.cuy.getWorld().isAir(place.up())
                                            && (ground.isSolidBlock(this.cuy.getWorld(), floor) || ground.isOf(Blocks.FARMLAND))) {
                                        double d = this.distance(place);
                                        if (d < best) {
                                            best = d;
                                            this.target = plant.toImmutable();
                                            this.standing = place.toImmutable();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return this.target != null;
        }
        return false;
    }

    @Override
    public void start() {
        this.running = true;
        this.deadline = this.cuy.age + 160;
        if (this.distance(this.standing) > 2.5) {
            this.running = this.cuy.getNavigation().startMovingTo(this.standing.getX() + 0.5, this.standing.getY(), this.standing.getZ() + 0.5, 0.9);
        }
    }

    @Override
    public boolean shouldContinue() {
        return this.running && this.cuy.age < this.deadline && !this.cuy.getCuyFleeing() && !this.cuy.isCuyBusy()
                && this.cuy.getCustomer() == null && this.cuy.getCuyFarm().isWorkingTime() && this.cuy.getCuyFarm().getStation() != null;
    }

    @Override
    public boolean shouldRunEveryTick() { return true; }

    @Override
    public void tick() {
        if (this.target != null && this.standing != null) {
            this.cuy.getLookControl().lookAt(this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5);
            if (this.distance(this.standing) <= 2.5) {
                this.cuy.getNavigation().stop();
                this.running = false;
                if (this.cuy.getCuyFarm().harvest(this.target)) {
                    this.cuy.startGardenAction("gardening", 48);
                }
            } else if (this.cuy.getNavigation().isIdle()) {
                this.running = false;
            }
        } else {
            this.running = false;
        }
    }

    @Override
    public void stop() {
        this.running = false;
        this.target = null;
        this.standing = null;
        this.cuy.getNavigation().stop();
    }
}
