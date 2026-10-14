package pe.apuyawar.systems.network;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** C2S: teletransportar al Staff que lo pide hacia un jugador conectado. */
public record AdminTeleportToPayload(String targetName) implements CustomPayload {
    public static final Id<AdminTeleportToPayload> ID = new Id<>(Identifier.of("apuyawar_systems", "admin_teleport_to"));
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, AdminTeleportToPayload> CODEC = PacketCodec.of(
            (payload, buffer) -> PacketCodecs.STRING.encode(buffer, payload.targetName()),
            buffer -> new AdminTeleportToPayload(PacketCodecs.STRING.decode(buffer))
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
