package pe.apuyawar.systems.missions;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Progreso de misiones por jugador, persistido en el nivel del mundo. Servidor-autoritativo. */
public final class MissionState extends PersistentState {
    private static final String KEY = "apuyawar_missions";
    private final Map<UUID, Map<String, Integer>> progress = new HashMap<>();
    private final Map<UUID, Set<String>> rewarded = new HashMap<>();

    public static final Type<MissionState> TYPE = new Type<>(
            MissionState::new,
            (nbt, registryLookup) -> {
                MissionState state = new MissionState();
                NbtList players = nbt.getList("players", NbtElement.COMPOUND_TYPE);
                for (int i = 0; i < players.size(); i++) {
                    NbtCompound entry = players.getCompound(i);
                    UUID uuid;
                    try { uuid = UUID.fromString(entry.getString("uuid")); } catch (IllegalArgumentException e) { continue; }
                    Map<String, Integer> missionProgress = new HashMap<>();
                    NbtCompound progressNbt = entry.getCompound("progress");
                    for (String id : progressNbt.getKeys()) missionProgress.put(id, progressNbt.getInt(id));
                    state.progress.put(uuid, missionProgress);
                    Set<String> rewardedSet = new HashSet<>();
                    NbtList rewardedNbt = entry.getList("rewarded", NbtElement.STRING_TYPE);
                    for (int j = 0; j < rewardedNbt.size(); j++) rewardedSet.add(rewardedNbt.getString(j));
                    state.rewarded.put(uuid, rewardedSet);
                }
                return state;
            },
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    public static MissionState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, KEY);
    }

    public int progress(UUID player, String missionId) {
        return progress.getOrDefault(player, Map.of()).getOrDefault(missionId, 0);
    }
    public boolean isRewarded(UUID player, String missionId) {
        return rewarded.getOrDefault(player, Set.of()).contains(missionId);
    }
    /** Incrementa el progreso; devuelve el nuevo valor. No otorga la recompensa por sí solo. */
    public int increment(UUID player, String missionId, int amount) {
        int value = progress.computeIfAbsent(player, k -> new HashMap<>())
                .merge(missionId, amount, Integer::sum);
        markDirty();
        return value;
    }
    public void markRewarded(UUID player, String missionId) {
        rewarded.computeIfAbsent(player, k -> new HashSet<>()).add(missionId);
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList players = new NbtList();
        Set<UUID> allPlayers = new HashSet<>(progress.keySet());
        allPlayers.addAll(rewarded.keySet());
        for (UUID uuid : allPlayers) {
            NbtCompound entry = new NbtCompound();
            entry.putString("uuid", uuid.toString());
            NbtCompound progressNbt = new NbtCompound();
            for (var e : progress.getOrDefault(uuid, Map.of()).entrySet()) progressNbt.putInt(e.getKey(), e.getValue());
            entry.put("progress", progressNbt);
            NbtList rewardedNbt = new NbtList();
            for (String id : rewarded.getOrDefault(uuid, Set.of())) rewardedNbt.add(NbtString.of(id));
            entry.put("rewarded", rewardedNbt);
            players.add(entry);
        }
        nbt.put("players", players);
        return nbt;
    }
}
