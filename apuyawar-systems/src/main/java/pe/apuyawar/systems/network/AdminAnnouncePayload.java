package pe.apuyawar.systems.network;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** C2S: pedido de anuncio global desde el panel administrativo. El servidor vuelve
 * a verificar Staff+Creative antes de difundirlo; el cliente nunca lo hace directo. */
public record AdminAnnouncePayload(String message) implements CustomPayload {
    public static final Id<AdminAnnouncePayload> ID = new Id<>(Identifier.of("apuyawar_systems", "admin_announce"));
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, AdminAnnouncePayload> CODEC = PacketCodec.of(
            (payload, buffer) -> PacketCodecs.STRING.encode(buffer, payload.message()),
            buffer -> new AdminAnnouncePayload(PacketCodecs.STRING.decode(buffer))
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
