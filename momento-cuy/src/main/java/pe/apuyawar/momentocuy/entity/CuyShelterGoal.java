package pe.apuyawar.momentocuy.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.Goal.Control;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.EnumSet;

/** Adaptado sin cambios de comportamiento del {@code CuyShelterGoal} del mod
 * antiguo (ver {@code CuyJardineroEntity} para el proceso de recuperación). */
public final class CuyShelterGoal extends Goal {
    private final CuyJardineroEntity cuy;
    private BlockPos gate;
    private BlockPos outside;
    private BlockPos inside;
    private boolean running;
    private boolean sheltered;
    private int nextSearch;
    private int deadline;
    private int lastMove;

    public CuyShelterGoal(CuyJardineroEntity cuy) {
        this.cuy = cuy;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private boolean needsShelter() {
        long dayTime = Math.floorMod(this.cuy.getWorld().getTimeOfDay(), 24000L);
        boolean night = dayTime >= 12500L && dayTime < 23000L;
        return night || this.cuy.getCuyFleeing();
    }

    private boolean passable(BlockPos pos) {
        World world = this.cuy.getWorld();
        return world.isAir(pos) && world.isAir(pos.up()) && world.getBlockState(pos.down()).isSolidBlock(world, pos.down());
    }

    private boolean roofed(BlockPos pos) {
        World world = this.cuy.getWorld();
        for (int height = 2; height <= 3; height++) {
            BlockPos roof = pos.up(height);
            if (world.getBlockState(roof).isSolidBlock(world, roof)) return true;
        }
        return false;
    }

    private boolean validFrame(BlockPos pos, Direction facing) {
        World world = this.cuy.getWorld();
        Direction left = facing.rotateYCounterclockwise();
        Direction right = facing.rotateYClockwise();
        return this.isWall(world, pos.offset(left)) && this.isWall(world, pos.offset(right));
    }

    private boolean isWall(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock || state.isSolidBlock(world, pos);
    }

    private double distance(BlockPos pos) {
        return this.cuy.squaredDistanceTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    @Override
    public boolean canStart() {
        if (this.needsShelter() && this.cuy.age >= this.nextSearch) {
            this.nextSearch = this.cuy.age + 100;
            this.gate = null;
            this.inside = null;
            this.outside = null;
            BlockPos center = this.cuy.getBlockPos();
            double best = Double.MAX_VALUE;
            for (int x = -10; x <= 10; x++) {
                for (int z = -10; z <= 10; z++) {
                    for (int y = -2; y <= 2; y++) {
                        BlockPos pos = center.add(x, y, z);
                        BlockState state = this.cuy.getWorld().getBlockState(pos);
                        if (state.getBlock() instanceof FenceGateBlock && !state.get(FenceGateBlock.POWERED)) {
                            Direction facing = state.get(FenceGateBlock.FACING);
                            if (this.validFrame(pos, facing)) {
                                for (Direction direction : new Direction[]{facing, facing.getOpposite()}) {
                                    BlockPos candidateInside = pos.offset(direction, 2);
                                    BlockPos candidateOutside = pos.offset(direction.getOpposite(), 2);
                                    if (this.passable(candidateInside) && this.passable(candidateOutside) && this.roofed(candidateInside)) {
                                        double score = this.distance(candidateOutside);
                                        if (score < best) {
                                            best = score;
                                            this.gate = pos.toImmutable();
                                            this.inside = candidateInside.toImmutable();
                                            this.outside = candidateOutside.toImmutable();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return this.gate != null;
        }
        return false;
    }

    @Override
    public void start() {
        this.running = true;
        this.sheltered = false;
        this.deadline = this.cuy.age + 240;
        this.lastMove = -1000;
        if (this.distance(this.inside) < 2.0) {
            this.sheltered = true;
            this.cuy.getNavigation().stop();
        } else {
            this.moveTo(this.outside, 1.25);
        }
    }

    private void moveTo(BlockPos pos, double speed) {
        this.lastMove = this.cuy.age;
        this.cuy.getNavigation().startMovingTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, speed);
    }

    @Override
    public boolean shouldContinue() {
        return this.running && this.needsShelter() && (this.sheltered || this.cuy.age < this.deadline);
    }

    @Override
    public boolean shouldRunEveryTick() { return true; }

    @Override
    public void tick() {
        if (this.inside == null || this.gate == null) {
            this.running = false;
        } else if (this.sheltered) {
            this.cuy.getNavigation().stop();
        } else if (this.distance(this.inside) < 2.0) {
            this.sheltered = true;
            this.cuy.getNavigation().stop();
        } else if (this.distance(this.outside) < 5.0) {
            if (this.cuy.openShelterGate(this.gate) && this.cuy.age - this.lastMove >= 10) {
                this.moveTo(this.inside, 1.3);
            }
        } else if (this.cuy.getNavigation().isIdle() && this.cuy.age - this.lastMove >= 20) {
            this.moveTo(this.outside, 1.25);
        }
    }

    @Override
    public void stop() {
        this.cuy.getNavigation().stop();
        this.running = false;
        this.sheltered = false;
    }
}
