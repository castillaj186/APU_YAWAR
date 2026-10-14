package pe.apuyawar.systems.network;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** C2S: entregar Sol PE a un jugador conectado, desde el panel administrativo.
 * Se vuelve a verificar Staff+Creative en el servidor antes de entregar nada. */
public record AdminGiveSolPePayload(String targetName, int amount) implements CustomPayload {
    public static final Id<AdminGiveSolPePayload> ID = new Id<>(Identifier.of("apuyawar_systems", "admin_give_sol_pe"));
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, AdminGiveSolPePayload> CODEC = PacketCodec.of(
            (payload, buffer) -> { PacketCodecs.STRING.encode(buffer, payload.targetName()); PacketCodecs.VAR_INT.encode(buffer, payload.amount()); },
            buffer -> new AdminGiveSolPePayload(PacketCodecs.STRING.decode(buffer), PacketCodecs.VAR_INT.decode(buffer))
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
