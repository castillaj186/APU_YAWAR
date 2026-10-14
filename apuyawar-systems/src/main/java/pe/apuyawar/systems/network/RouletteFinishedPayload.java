package pe.apuyawar.systems.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Solo acuse visual. El cliente nunca elige ganador, reto ni dificultad. */
public record RouletteFinishedPayload(long sequence) implements CustomPayload {
    public static final Id<RouletteFinishedPayload> ID=new Id<>(Identifier.of("apuyawar_systems","roulette_finished"));
    public static final PacketCodec<RegistryByteBuf,RouletteFinishedPayload> CODEC=PacketCodec.of((p,b)->b.writeVarLong(p.sequence()),b->new RouletteFinishedPayload(b.readVarLong()));
    @Override public Id<? extends CustomPayload> getId(){return ID;}
}
