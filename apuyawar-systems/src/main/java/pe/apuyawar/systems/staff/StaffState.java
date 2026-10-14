package pe.apuyawar.systems.staff;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Rango de Staff de APU YAWAR: persistido en el nivel del mundo (no en el cliente),
 * autoritativo en el servidor. Estar en Creative NO otorga este rango por sí solo;
 * el rango es la única fuente de verdad para el panel administrativo.
 */
public final class StaffState extends PersistentState {
    private static final String KEY = "apuyawar_staff";
    private final Set<UUID> staff = new HashSet<>();

    public static final Type<StaffState> TYPE = new Type<>(
            StaffState::new,
            (nbt, registryLookup) -> {
                StaffState state = new StaffState();
                NbtList list = nbt.getList("staff", NbtElement.STRING_TYPE);
                for (int i = 0; i < list.size(); i++) {
                    try { state.staff.add(UUID.fromString(list.getString(i))); }
                    catch (IllegalArgumentException ignored) {}
                }
                return state;
            },
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    public static StaffState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, KEY);
    }

    public boolean isStaff(UUID uuid) { return staff.contains(uuid); }
    public boolean add(UUID uuid) { boolean changed = staff.add(uuid); if (changed) markDirty(); return changed; }
    public boolean remove(UUID uuid) { boolean changed = staff.remove(uuid); if (changed) markDirty(); return changed; }
    public Set<UUID> all() { return Collections.unmodifiableSet(staff); }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (UUID id : staff) list.add(NbtString.of(id.toString()));
        nbt.put("staff", list);
        return nbt;
    }
}
