package pe.apuyawar.momentocuy.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Adaptado sin cambios de comportamiento del {@code CuyFarmController} del
 * mod antiguo (ver {@code CuyJardineroEntity} para el proceso de
 * recuperación): canasta de 27 espacios, cosecha real de cultivos maduros
 * cerca de un compostador asignado, y compostaje automático de semillas
 * sobrantes. Claves NBT renombradas de {@code SafioCuyFarm*} a
 * {@code MomentoCuyFarm*} (datos nuevos de este mod, no hay guardado antiguo
 * que migrar). */
public final class CuyFarmController {
    private final CuyJardineroEntity cuy;
    private final SimpleInventory basket = new SimpleInventory(27);
    private BlockPos station;

    public CuyFarmController(CuyJardineroEntity cuy) { this.cuy = cuy; }

    public SimpleInventory getInventory() { return this.basket; }

    public String toggleNearbyStation() {
        BlockPos center = this.cuy.getBlockPos();
        BlockPos closest = null;
        double best = Double.MAX_VALUE;
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos pos = center.add(x, y, z);
                    if (this.cuy.getWorld().getBlockState(pos).isOf(Blocks.COMPOSTER)) {
                        double d = this.cuy.squaredDistanceTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                        if (d < best && d <= 64.0) {
                            best = d;
                            closest = pos.toImmutable();
                        }
                    }
                }
            }
        }
        if (closest == null) return "Coloca un compostador a menos de 8 bloques.";
        if (closest.equals(this.station)) {
            this.station = null;
            return "Granja desactivada: el Cuy ya no cosechara.";
        }
        this.station = closest;
        return "Compostador asignado: el Cuy ya puede cultivar.";
    }

    public BlockPos getStation() {
        if (this.station == null) return null;
        if (this.cuy.squaredDistanceTo(this.station.getX() + 0.5, this.station.getY(), this.station.getZ() + 0.5) > 400.0) return null;
        return this.cuy.getWorld().getBlockState(this.station).isOf(Blocks.COMPOSTER) ? this.station : null;
    }

    public boolean isWorkingTime() {
        long time = Math.floorMod(this.cuy.getWorld().getTimeOfDay(), 24000L);
        return time < 12500L || time >= 23000L;
    }

    private SimpleInventory copyBasket() {
        SimpleInventory result = new SimpleInventory(27);
        for (int i = 0; i < this.basket.size(); i++) result.setStack(i, this.basket.getStack(i).copy());
        return result;
    }

    public boolean harvest(BlockPos pos) {
        if (!(this.cuy.getWorld() instanceof ServerWorld world)) return false;
        BlockPos work = this.getStation();
        if (work == null || !this.isWorkingTime() || this.cuy.getCuyFleeing() || this.cuy.getCustomer() != null) return false;
        if (Math.abs(pos.getX() - work.getX()) > 5 || Math.abs(pos.getZ() - work.getZ()) > 5 || Math.abs(pos.getY() - work.getY()) > 2) return false;
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMature(state) || !world.getBlockState(pos.down()).isOf(Blocks.FARMLAND)) {
            return false;
        }
        Item seed;
        if (state.isOf(Blocks.WHEAT)) seed = Items.WHEAT_SEEDS;
        else if (state.isOf(Blocks.BEETROOTS)) seed = Items.BEETROOT_SEEDS;
        else if (state.isOf(Blocks.CARROTS)) seed = Items.CARROT;
        else if (state.isOf(Blocks.POTATOES)) seed = Items.POTATO;
        else return false;

        List<ItemStack> result = new ArrayList<>();
        for (ItemStack dropped : Block.getDroppedStacks(state, world, pos, null, this.cuy, ItemStack.EMPTY)) {
            if (!dropped.isEmpty()) result.add(dropped.copy());
        }
        boolean seedReserved = false;
        for (ItemStack stack : result) {
            if (stack.isOf(seed) && stack.getCount() > 0) {
                stack.decrement(1);
                seedReserved = true;
                break;
            }
        }
        SimpleInventory trial = this.copyBasket();
        if (!seedReserved && trial.removeItem(seed, 1).isEmpty()) return false;
        for (ItemStack stack : result) {
            if (!stack.isEmpty() && !trial.addStack(stack.copy()).isEmpty()) return false;
        }
        if (!world.setBlockState(pos, crop.getDefaultState(), 3)) return false;
        for (int i = 0; i < this.basket.size(); i++) this.basket.setStack(i, trial.getStack(i).copy());
        this.basket.markDirty();
        world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
        return true;
    }

    public void tick() {
        if (this.cuy.getWorld() instanceof ServerWorld world
                && this.cuy.age % 80 == 0 && !this.cuy.getCuyFleeing() && !this.cuy.isCuyBusy()
                && this.cuy.getCustomer() == null && this.isWorkingTime()) {
            BlockPos pos = this.getStation();
            if (pos != null && !(this.cuy.squaredDistanceTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 16.0)) {
                BlockState composter = world.getBlockState(pos);
                if (composter.isOf(Blocks.COMPOSTER) && composter.get(ComposterBlock.LEVEL) < 7) {
                    for (int slot = 0; slot < this.basket.size(); slot++) {
                        ItemStack stack = this.basket.getStack(slot);
                        if (stack.getCount() > 48
                                && (stack.isOf(Items.WHEAT_SEEDS) || stack.isOf(Items.BEETROOT_SEEDS) || stack.isOf(Items.CARROT) || stack.isOf(Items.POTATO))) {
                            ItemStack one = stack.copyWithCount(1);
                            ComposterBlock.compost(this.cuy, composter, world, one, pos);
                            this.basket.removeStack(slot, 1);
                            this.basket.markDirty();
                            break;
                        }
                    }
                }
            }
        }
    }

    public void write(NbtCompound nbt) {
        nbt.putBoolean("MomentoCuyFarmAssigned", this.station != null);
        if (this.station != null) nbt.putLong("MomentoCuyFarmStation", this.station.asLong());
        nbt.put("MomentoCuyFarmInventory", this.basket.toNbtList(this.cuy.getWorld().getRegistryManager()));
    }

    public void read(NbtCompound nbt) {
        this.station = nbt.getBoolean("MomentoCuyFarmAssigned") && nbt.contains("MomentoCuyFarmStation")
                ? BlockPos.fromLong(nbt.getLong("MomentoCuyFarmStation")) : null;
        this.basket.clear();
        if (nbt.contains("MomentoCuyFarmInventory", 9)) {
            this.basket.readNbtList(nbt.getList("MomentoCuyFarmInventory", 10), this.cuy.getWorld().getRegistryManager());
        }
    }
}
