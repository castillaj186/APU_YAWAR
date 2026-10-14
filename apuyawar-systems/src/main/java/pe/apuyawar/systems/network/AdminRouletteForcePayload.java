package pe.apuyawar.systems.network;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** C2S: lanzar la ruleta con un color forzado desde el panel administrativo,
 * reutilizando exactamente la misma lógica de servidor que ya usa el comando
 * {@code /apuyawar_ruleta forzar}; no se reescribe el renderizador ni la animación. */
public record AdminRouletteForcePayload(int colorIndex) implements CustomPayload {
    public static final Id<AdminRouletteForcePayload> ID = new Id<>(Identifier.of("apuyawar_systems", "admin_roulette_force"));
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, AdminRouletteForcePayload> CODEC = PacketCodec.of(
            (payload, buffer) -> PacketCodecs.VAR_INT.encode(buffer, payload.colorIndex()),
            buffer -> new AdminRouletteForcePayload(PacketCodecs.VAR_INT.decode(buffer))
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
