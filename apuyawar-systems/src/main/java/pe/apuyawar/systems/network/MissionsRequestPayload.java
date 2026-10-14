package pe.apuyawar.systems.network;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** C2S: el jugador abrió la pantalla de Misiones; pide su progreso real. */
public record MissionsRequestPayload() implements CustomPayload {
    public static final Id<MissionsRequestPayload> ID = new Id<>(Identifier.of("apuyawar_systems", "missions_request"));
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, MissionsRequestPayload> CODEC =
            PacketCodec.unit(new MissionsRequestPayload());
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
