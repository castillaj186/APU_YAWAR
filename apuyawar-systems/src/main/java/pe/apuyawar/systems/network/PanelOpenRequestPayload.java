package pe.apuyawar.systems.network;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** C2S: el jugador presionó K. El servidor decide qué panel corresponde. */
public record PanelOpenRequestPayload() implements CustomPayload {
    public static final Id<PanelOpenRequestPayload> ID = new Id<>(Identifier.of("apuyawar_systems", "panel_open_request"));
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, PanelOpenRequestPayload> CODEC =
            PacketCodec.unit(new PanelOpenRequestPayload());
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
