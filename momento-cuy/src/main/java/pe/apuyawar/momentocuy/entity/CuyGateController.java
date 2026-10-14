package pe.apuyawar.momentocuy.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Adaptado sin cambios de comportamiento del {@code CuyGateController} del
 * mod antiguo (ver {@code CuyJardineroEntity} para el proceso de
 * recuperación): abre/cierra puertas y portones reales al refugiarse. */
public final class CuyGateController {
    private final CuyJardineroEntity cuy;
    private BlockPos ownedGate;
    private int openedAt;

    public CuyGateController(CuyJardineroEntity cuy) { this.cuy = cuy; }

    private static boolean supported(BlockState state) {
        return state.getBlock() instanceof FenceGateBlock || (state.getBlock() instanceof DoorBlock && DoorBlock.canOpenByHand(state));
    }

    private static boolean powered(BlockState state) {
        if (state.getBlock() instanceof FenceGateBlock) return state.get(FenceGateBlock.POWERED);
        return state.getBlock() instanceof DoorBlock && state.get(DoorBlock.POWERED);
    }

    private static boolean opened(BlockState state) {
        if (state.getBlock() instanceof FenceGateBlock) return state.get(FenceGateBlock.OPEN);
        return state.getBlock() instanceof DoorBlock && state.get(DoorBlock.OPEN);
    }

    private void change(BlockPos pos, boolean open) {
        World world = this.cuy.getWorld();
        BlockState state = world.getBlockState(pos);
        if (supported(state) && !powered(state)) {
            if (state.getBlock() instanceof DoorBlock door) {
                door.setOpen(this.cuy, world, state, pos, open);
            } else if (state.getBlock() instanceof FenceGateBlock) {
                world.setBlockState(pos, state.with(FenceGateBlock.OPEN, open), 3);
            }
        }
    }

    private double distanceSquared(BlockPos pos) {
        return this.cuy.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public boolean openForShelter(BlockPos pos) {
        World world = this.cuy.getWorld();
        BlockState state = world.getBlockState(pos);
        if (!supported(state) || powered(state)) return false;
        if (this.ownedGate != null && !this.ownedGate.equals(pos)) return false;
        if (!opened(state)) {
            this.change(pos, true);
            if (!opened(world.getBlockState(pos))) return false;
            this.ownedGate = pos.toImmutable();
            this.openedAt = this.cuy.age;
        }
        return true;
    }

    public void tick() {
        World world = this.cuy.getWorld();
        if (!world.isClient && this.cuy.age % 5 == 0) {
            if (this.ownedGate != null) {
                BlockState state = world.getBlockState(this.ownedGate);
                if (supported(state) && opened(state)) {
                    double distance = this.distanceSquared(this.ownedGate);
                    int elapsed = this.cuy.age - this.openedAt;
                    boolean passed = distance > 6.25;
                    boolean timeout = elapsed > 80 && distance > 1.5;
                    if ((!passed && !timeout) || powered(state)) {
                        return;
                    }
                    this.change(this.ownedGate, false);
                    this.ownedGate = null;
                } else {
                    this.ownedGate = null;
                }
            }
            if (!this.cuy.isCuyBusy() || this.cuy.getCuyFleeing()) {
                Vec3d velocity = this.cuy.getVelocity();
                double speed = velocity.x * velocity.x + velocity.z * velocity.z;
                if (!(speed < 4.0E-4) || !this.cuy.getNavigation().isIdle()) {
                    BlockPos center = this.cuy.getBlockPos();
                    BlockPos nearest = null;
                    double best = 4.0;
                    for (int x = -2; x <= 2; x++) {
                        for (int z = -2; z <= 2; z++) {
                            for (int y = -1; y <= 1; y++) {
                                BlockPos pos = center.add(x, y, z);
                                BlockState state = world.getBlockState(pos);
                                if (supported(state) && !powered(state) && !opened(state)) {
                                    double distance = this.distanceSquared(pos);
                                    if (!(distance >= best)) {
                                        double dx = pos.getX() + 0.5 - this.cuy.getX();
                                        double dz = pos.getZ() + 0.5 - this.cuy.getZ();
                                        double toward = velocity.x * dx + velocity.z * dz;
                                        if (!(speed > 4.0E-4) || !(toward < 0.0)) {
                                            best = distance;
                                            nearest = pos.toImmutable();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (nearest != null) {
                        this.change(nearest, true);
                        this.ownedGate = nearest;
                        this.openedAt = this.cuy.age;
                    }
                }
            }
        }
    }
}
