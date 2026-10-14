package pe.apuyawar.systems.events;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class JudgmentPresentation {
    private JudgmentPresentation(){}
    public static void announce(ServerPlayerEntity player,String title,String subtitle,String details,boolean orange){
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10,65,15));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(subtitle).formatted(Formatting.GOLD)));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(title).formatted(orange?Formatting.GOLD:Formatting.RED)));
        player.sendMessage(Text.literal("APU YAWAR | "+details).formatted(orange?Formatting.GOLD:Formatting.RED),false);
    }
}
