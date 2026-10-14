import java.util.*;
import pe.apuyawar.systems.roulette.*;
import pe.apuyawar.systems.client.RouletteParticles;

public class ValidateRoulette {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        for(int winner=0;winner<8;winner++){
            RouletteEvents clock=new RouletteEvents();List<RouletteEvents.Event> events=new ArrayList<>();
            for(int tick=0;tick<=310;tick++){
                clock.advance(tick,winner,events::add);
                int size=events.size();clock.advance(tick,winner,events::add);
                check(events.size()==size,"duplicate tick");
            }
            for(String type:List.of("appear","fill","result","finish","outro"))
                check(events.stream().filter(e->e.name().equals(type)).count()==(type.equals("appear")||type.equals("fill")?8:1),type);
            var fill=events.stream().filter(e->e.name().equals("fill")).toList();
            double finalRotation=RouletteTimeline.rotationDegrees(212,winner);
            check(RouletteTimeline.slotUnderPointer(finalRotation)==winner,"pointer");
            for(int rank=0;rank<8;rank++){
                int slot=fill.get(rank).slot();check(slot==(winner+rank)%8,"clockwise order");
                check(RouletteTimeline.fillBlend(fill.get(rank).tick()+4,slot,winner)==1,"completed texture");
                if(rank>0){double a=finalRotation-67.5+fill.get(rank-1).slot()*45;
                    double b=finalRotation-67.5+slot*45;
                    check(Math.floorMod((int)Math.round(b-a),360)==45,"actual visual neighbor");}
            }
            double previous=-100;
            for(var e:events)if(e.name().equals("tick")){
                double angle=RouletteTimeline.rotationDegrees(e.tick(),winner);
                double edge=(angle-270)/45;
                check(Math.abs(edge-Math.rint(edge))<.00001,"true boundary");
                check(e.tick()-previous>=1.9999,"audio limit");previous=e.tick();
            }
            RouletteParticles.clear();
            for(var e:events){RouletteParticles.prune(e.tick());RouletteParticles.emit(e,winner);check(RouletteParticles.active().size()<=80,"particle cap");}
            RouletteParticles.prune(400);check(RouletteParticles.active().isEmpty(),"particle expiry");
            clock.reset();events.clear();clock.advance(2,winner,events::add);check(events.size()==1,"restart");
        }
        System.out.println("PASS: eight results, clockwise geometry, single events, real boundary ticks, rate cap, particle expiry/restart.");
    }
}
