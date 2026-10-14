package pe.apuyawar.systems.network;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: el servidor ya validó el rango de Staff y el modo de juego real del jugador
 * y decide qué panel debe abrirse. El cliente nunca decide esto por sí mismo.
 */
public record PanelOpenResponsePayload(boolean adminPanel) implements CustomPayload {
    public static final Id<PanelOpenResponsePayload> ID = new Id<>(Identifier.of("apuyawar_systems", "panel_open_response"));
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, PanelOpenResponsePayload> CODEC = PacketCodec.of(
            (payload, buffer) -> buffer.writeBoolean(payload.adminPanel()),
            buffer -> new PanelOpenResponsePayload(buffer.readBoolean())
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
