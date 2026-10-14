package pe.apuyawar.systems.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Starts the local Inti presentation. The server retains all challenge authority. */
public record IntiStartPayload(long sequence, boolean previewOnly) implements CustomPayload {
    public static final Id<IntiStartPayload> ID = new Id<>(Identifier.of("apuyawar_systems", "inti_start"));
    public static final PacketCodec<RegistryByteBuf, IntiStartPayload> CODEC = PacketCodec.of(
        (payload, buffer) -> { buffer.writeVarLong(payload.sequence()); buffer.writeBoolean(payload.previewOnly()); },
        buffer -> new IntiStartPayload(buffer.readVarLong(), buffer.readBoolean())
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
