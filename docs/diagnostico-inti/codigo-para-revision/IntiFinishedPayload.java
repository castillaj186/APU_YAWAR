package pe.apuyawar.systems.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** A single acknowledgement emitted after the client completed Inti's presentation. */
public record IntiFinishedPayload(long sequence) implements CustomPayload {
    public static final Id<IntiFinishedPayload> ID = new Id<>(Identifier.of("apuyawar_systems", "inti_finished"));
    public static final PacketCodec<RegistryByteBuf, IntiFinishedPayload> CODEC = PacketCodec.of(
        (payload, buffer) -> buffer.writeVarLong(payload.sequence()),
        buffer -> new IntiFinishedPayload(buffer.readVarLong())
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
