package pe.apuyawar.momentocuy.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.Goal.Control;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.EnumSet;
import java.util.Random;

/** Adaptado sin cambios de comportamiento del {@code CuyGardenGoal} del mod
 * antiguo (ver {@code CuyJardineroEntity} para el proceso de recuperación). */
public final class CuyGardenGoal extends Goal {
    private final CuyJardineroEntity cuy;
    private final Random random = new Random();
    private BlockPos plant;
    private BlockPos standing;
    private String action;
    private int duration;
    private int retryAt;
    private int deadline;
    private boolean running;

    public CuyGardenGoal(CuyJardineroEntity cuy) {
        this.cuy = cuy;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private static boolean isCrop(BlockState state) {
        return state.isOf(Blocks.WHEAT) || state.isOf(Blocks.CARROTS) || state.isOf(Blocks.POTATOES) || state.isOf(Blocks.BEETROOTS);
    }

    private static boolean isFlower(BlockState state) {
        return state.isOf(Blocks.DANDELION) || state.isOf(Blocks.POPPY) || state.isOf(Blocks.CORNFLOWER)
                || state.isOf(Blocks.OXEYE_DAISY) || state.isOf(Blocks.ALLIUM);
    }

    @Override
    public boolean canStart() {
        if (!this.cuy.isCuyBusy() && !this.cuy.getCuyFleeing() && this.cuy.isOnGround() && this.cuy.age >= this.retryAt) {
            this.retryAt = this.cuy.age + 100 + this.random.nextInt(100);
            this.plant = null;
            this.standing = null;
            BlockPos center = this.cuy.getBlockPos();
            double best = Double.MAX_VALUE;
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    for (int y = -1; y <= 1; y++) {
                        BlockPos pos = center.add(x, y, z);
                        BlockState state = this.cuy.getWorld().getBlockState(pos);
                        boolean crop = isCrop(state);
                        boolean flower = isFlower(state);
                        if (crop || flower) {
                            for (Direction dir : Direction.Type.HORIZONTAL) {
                                BlockPos spot = pos.offset(dir);
                                BlockPos floor = spot.down();
                                if (this.cuy.getWorld().isAir(spot) && this.cuy.getWorld().isAir(spot.up())) {
                                    BlockState ground = this.cuy.getWorld().getBlockState(floor);
                                    if (ground.isSolidBlock(this.cuy.getWorld(), floor) || ground.isOf(Blocks.FARMLAND)) {
                                        double d = this.cuy.squaredDistanceTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
                                        if (d < best) {
                                            best = d;
                                            this.plant = pos;
                                            this.standing = spot;
                                            if (crop) {
                                                switch (this.random.nextInt(3)) {
                                                    case 0 -> { this.action = "inspect_crop"; this.duration = 60; }
                                                    case 1 -> { this.action = "water_plants"; this.duration = 90; }
                                                    default -> { this.action = "gardening"; this.duration = 48; }
                                                }
                                            } else {
                                                this.action = "sniff_flower";
                                                this.duration = 70;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return this.standing != null;
        }
        return false;
    }

    private boolean closeEnough() {
        return this.standing != null
                && this.cuy.squaredDistanceTo(this.standing.getX() + 0.5, this.standing.getY(), this.standing.getZ() + 0.5) <= 1.7;
    }

    @Override
    public void start() {
        this.running = true;
        this.deadline = this.cuy.age + 100;
        this.cuy.setWalkingToGarden(true);
        if (this.closeEnough()) {
            this.finishTask();
        } else {
            boolean foundPath = this.cuy.getNavigation()
                    .startMovingTo(this.standing.getX() + 0.5, this.standing.getY(), this.standing.getZ() + 0.5, 0.85);
            if (!foundPath) {
                this.running = false;
                this.cuy.setWalkingToGarden(false);
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        return this.running && !this.cuy.isCuyBusy() && !this.cuy.getCuyFleeing() && this.cuy.age < this.deadline
                && (this.closeEnough() || !this.cuy.getNavigation().isIdle());
    }

    @Override
    public boolean shouldRunEveryTick() { return true; }

    @Override
    public void tick() {
        if (this.plant != null) {
            this.cuy.getLookControl().lookAt(this.plant.getX() + 0.5, this.plant.getY() + 0.5, this.plant.getZ() + 0.5);
            if (this.closeEnough()) this.finishTask();
        }
    }

    private void finishTask() {
        this.cuy.getNavigation().stop();
        this.running = false;
        this.cuy.setWalkingToGarden(false);
        this.cuy.startGardenAction(this.action, this.duration);
    }

    @Override
    public void stop() {
        this.running = false;
        this.cuy.setWalkingToGarden(false);
        this.cuy.getNavigation().stop();
        this.plant = null;
        this.standing = null;
    }
}
