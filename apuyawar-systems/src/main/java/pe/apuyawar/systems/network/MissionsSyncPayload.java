package pe.apuyawar.systems.network;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import pe.apuyawar.systems.missions.MissionCatalog;

/**
 * S2C: progreso real del jugador para cada misión, en el mismo orden que
 * {@code MissionCatalog.ALL} (compartido entre cliente y servidor en el mismo jar,
 * así que nombre/descripción/requisito/recompensa no necesitan reenviarse).
 */
public record MissionsSyncPayload(int[] progress, boolean[] rewarded) implements CustomPayload {
    public static final Id<MissionsSyncPayload> ID = new Id<>(Identifier.of("apuyawar_systems", "missions_sync"));
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, MissionsSyncPayload> CODEC = PacketCodec.of(
            (payload, buffer) -> {
                for (int value : payload.progress()) buffer.writeVarInt(value);
                for (boolean value : payload.rewarded()) buffer.writeBoolean(value);
            },
            buffer -> {
                int count = MissionCatalog.ALL.size();
                int[] progress = new int[count];
                for (int i = 0; i < count; i++) progress[i] = buffer.readVarInt();
                boolean[] rewarded = new boolean[count];
                for (int i = 0; i < count; i++) rewarded[i] = buffer.readBoolean();
                return new MissionsSyncPayload(progress, rewarded);
            }
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
