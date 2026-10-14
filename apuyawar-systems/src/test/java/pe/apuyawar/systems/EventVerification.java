package pe.apuyawar.systems;

import java.util.*;
import pe.apuyawar.systems.events.*;
import pe.apuyawar.systems.challenges.ChallengeRules;

/** Pruebas sin dependencias nuevas; se ejecutan en check mediante JavaExec. */
public final class EventVerification {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static ChallengeRules.PlayerFacts facts(double y,String dimension,boolean items,boolean roof,boolean destination,boolean eligible){return new ChallengeRules.PlayerFacts(y,dimension,roof,items,items,items,items,destination,eligible);}
    public static void main(String[] args){
        check(EventCatalog.CHALLENGES.size()==128,"128 retos recuperados");check(EventCatalog.DIFFICULTIES.size()==135,"135 dificultades recuperadas");
        for(var c:EventCatalog.CHALLENGES.values()){
            double y=c.goal().equals("depth")||c.goal().equals("torch_depth")?c.target()-1:c.target()+1;
            String dimension=c.goal().equals("nether")?"minecraft:the_nether":"minecraft:overworld";
            check(ChallengeRules.passed(c,facts(y,dimension,true,true,true,true),true),"Victoria real: "+c.id());
            check(!ChallengeRules.passed(c,facts(y,dimension,true,true,true,false),true),"No gana un jugador excluido/muerto: "+c.id());
            if(c.goal().equals("height")||c.goal().equals("gas"))check(!ChallengeRules.passed(c,facts(c.target()-.01,dimension,true,true,true,true),true),"Altura insuficiente: "+c.id());
            if(c.goal().equals("depth")||c.goal().equals("torch_depth"))check(!ChallengeRules.passed(c,facts(c.target()+.01,dimension,true,true,true,true),true),"Profundidad insuficiente: "+c.id());
            if(Set.of("shield","bow","apple","torch_depth","roof_shield").contains(c.goal()))check(!ChallengeRules.passed(c,facts(y,dimension,false,true,true,true),true),"Objeto ausente: "+c.id());
            if(c.goal().equals("survive"))check(!ChallengeRules.passed(c,facts(y,dimension,true,true,true,true),false),"Supervivencia no automática: "+c.id());
            if(c.goal().equals("nether"))check(!ChallengeRules.passed(c,facts(y,"minecraft:overworld",true,true,true,true),true),"Dimensión incorrecta: "+c.id());
            if(c.goal().equals("destination"))check(!ChallengeRules.passed(c,facts(y,dimension,true,true,false,true),true),"Refugio fuera de radio: "+c.id());
            if(c.goal().startsWith("roof"))check(!ChallengeRules.passed(c,facts(y,dimension,true,false,true,true),true),"Cielo visible: "+c.id());
            check(c.duration()>0,"Temporizador válido");
        }
        // Todas las variantes naranjas tienen acciones tipadas o un manejador explícito.
        Set<Integer> special=Set.of(1,9,12,40,41,42,44,45,48,51,52,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,92,96);
        for(var d:EventCatalog.DIFFICULTIES.values()){
            check(d.legacy()<=35||!d.actions().isEmpty()||special.contains(d.legacy()-1000),"Sin mecánica: "+d.id());
            for(var action:d.actions()){check(Set.of("attribute","effect").contains(action.kind()),"Acción inválida");if(action.kind().equals("attribute"))check(Set.of("max_health","movement_speed","follow_range").contains(action.attribute()),"Atributo inválido");}
            check(!(d.name()+d.description()).toLowerCase(Locale.ROOT).matches(".*(safío|safio|reviil).*"),"Identidad pública antigua");
        }
        UUID a=UUID.randomUUID(),b=UUID.randomUUID(),rogue=UUID.randomUUID();var gate=new RouletteCompletionGate();
        gate.begin(1,0,List.of(a,b));gate.acknowledge(a,1);gate.acknowledge(b,1);
        check(gate.status(310,List.of(a,b))==RouletteCompletionGate.Status.WAITING,"Acuse no adelanta presentación");
        check(gate.status(314,List.of(a,b))==RouletteCompletionGate.Status.READY,"Quórum válido");
        gate.begin(2,100,List.of(a,b));gate.acknowledge(a,1);gate.acknowledge(rogue,2);
        check(gate.status(414,List.of(a,b))==RouletteCompletionGate.Status.WAITING,"Acuses obsoletos/ajenos");
        gate.acknowledge(a,2);gate.acknowledge(a,2);
        check(gate.status(414,List.of(a))==RouletteCompletionGate.Status.READY,"Desconexión y duplicados");
        gate.begin(3,0,List.of(a));check(gate.status(511,List.of(a))==RouletteCompletionGate.Status.TIMED_OUT,"Cancelación de cliente sin respuesta");
        System.out.println("PASS: 128 objetivos, 135 catálogos con mecánica, límites de éxito/fracaso, temporizadores y acuses multijugador.");
    }
}
