package pe.apuyawar.systems.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Se envía UNA vez por giro: nunca se transmiten fotogramas. */
public record RouletteStartPayload(int colorIndex, long sequence) implements CustomPayload {
    public static final CustomPayload.Id<RouletteStartPayload> ID =
        new CustomPayload.Id<>(Identifier.of("apuyawar_systems", "roulette_start"));
    public static final PacketCodec<RegistryByteBuf, RouletteStartPayload> CODEC = PacketCodec.of(
        (payload, buf) -> { buf.writeVarInt(payload.colorIndex()); buf.writeVarLong(payload.sequence()); },
        buf -> new RouletteStartPayload(buf.readVarInt(), buf.readVarLong())
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
