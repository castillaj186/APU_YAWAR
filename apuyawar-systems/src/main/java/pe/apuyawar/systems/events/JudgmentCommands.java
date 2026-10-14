package pe.apuyawar.systems.events;

import com.mojang.brigadier.arguments.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.*;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import pe.apuyawar.systems.ApuYawarSystems;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;

public final class JudgmentCommands {
    private JudgmentCommands(){}
    public static void initialize(){CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->{
        var root=literal("apuyawar").requires(source->source.hasPermissionLevel(2));
        var challenges=literal("reto")
            .then(literal("listar").executes(c->list(c.getSource(),false,1)).then(argument("pagina",IntegerArgumentType.integer(1)).executes(c->list(c.getSource(),false,IntegerArgumentType.getInteger(c,"pagina")))))
            .then(literal("estado").executes(c->reply(c.getSource(),JudgmentService.get(c.getSource().getServer()).challenges.status())))
            .then(literal("detener").executes(c->{var s=JudgmentService.get(c.getSource().getServer());s.cancelPending();s.challenges.stop();return reply(c.getSource(),"Reto y activación pendiente detenidos.");}))
            .then(literal("iniciar").then(argument("id",StringArgumentType.word())
                .suggests((c,b)->CommandSource.suggestMatching(EventCatalog.CHALLENGES.keySet(),b))
                .executes(c->start(c.getSource(),StringArgumentType.getString(c,"id"),c.getSource().getServer().getPlayerManager().getPlayerList()))
                .then(argument("jugadores",EntityArgumentType.players()).executes(c->start(c.getSource(),StringArgumentType.getString(c,"id"),EntityArgumentType.getPlayers(c,"jugadores"))))));
        var difficulty=literal("dificultad")
            .then(literal("listar").executes(c->list(c.getSource(),true,1)).then(argument("pagina",IntegerArgumentType.integer(1)).executes(c->list(c.getSource(),true,IntegerArgumentType.getInteger(c,"pagina")))))
            .then(literal("estado").executes(c->reply(c.getSource(),JudgmentService.get(c.getSource().getServer()).difficulties.status())))
            .then(literal("detener").executes(c->{var s=JudgmentService.get(c.getSource().getServer());s.cancelPending();s.difficulties.stop();return reply(c.getSource(),"Dificultad y activación pendiente detenidas.");}))
            .then(literal("activar").then(argument("id",StringArgumentType.word()).suggests((c,b)->CommandSource.suggestMatching(EventCatalog.DIFFICULTIES.keySet(),b)).executes(c->{
                var s=JudgmentService.get(c.getSource().getServer());var selected=EventCatalog.DIFFICULTIES.get(StringArgumentType.getString(c,"id"));
                if(selected==null||s.pending()||!s.difficulties.start(selected)){c.getSource().sendError(Text.literal("APU YAWAR | ID inválido, sistema desactivado o evento ocupado."));return 0;}return 1;})));
        root.then(challenges).then(difficulty).then(literal("ruleta").then(literal("probar")
            .then(literal("rojo").executes(c->ApuYawarSystems.start(c.getSource(),0)))
            .then(literal("naranja").executes(c->ApuYawarSystems.start(c.getSource(),7)))))
            .then(literal("inti").then(literal("probar").executes(c->{
                JudgmentService.get(c.getSource().getServer()).playIntiPreview(c.getSource().getServer().getPlayerManager().getPlayerList());
                return reply(c.getSource(),"Presentación de Inti enviada a los clientes compatibles.");
            })));
        dispatcher.register(root);
    });}
    private static int start(ServerCommandSource source,String id,Collection<net.minecraft.server.network.ServerPlayerEntity> players){
        var service=JudgmentService.get(source.getServer());var selected=EventCatalog.CHALLENGES.get(id);
        if(selected==null||service.pending()||!service.challenges.start(selected,players)){source.sendError(Text.literal("APU YAWAR | ID inválido, sin participantes, sistema desactivado o reto ocupado."));return 0;}return 1;
    }
    private static int list(ServerCommandSource source,boolean orange,int page){
        List<String> lines=orange?EventCatalog.DIFFICULTIES.values().stream().map(d->d.id()+" — "+d.name()+": "+d.description()).toList():EventCatalog.CHALLENGES.values().stream().map(c->c.id()+" — "+c.name()+": "+c.description()).toList();
        int start=(page-1)*12;if(start>=lines.size()){source.sendError(Text.literal("Página inexistente."));return 0;}
        reply(source,"Catálogo: "+lines.size()+" entradas | página "+page+"/"+((lines.size()+11)/12));
        lines.subList(start,Math.min(start+12,lines.size())).forEach(line->reply(source,line));return 1;
    }
    private static int reply(ServerCommandSource source,String message){source.sendFeedback(()->Text.literal("APU YAWAR | "+message),false);return 1;}
}
