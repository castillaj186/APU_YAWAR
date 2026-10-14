package pe.apuyawar.systems.roulette;

import java.util.function.Consumer;

/** Cursor monotónico: eventos una sola vez, independientes de los fotogramas. */
public final class RouletteEvents {
    public record Event(String name, int slot, double tick) {}
    private double previous = -1, lastClick = -100;
    public void reset() { previous = -1; lastClick = -100; }
    public void advance(double now, int winner, Consumer<Event> emit) {
        if (now <= previous) return;
        for (int i=0;i<8;i++) {
            fire(now,"appear",i,RouletteTimeline.BUILD_START+i*RouletteTimeline.BUILD_STEP,emit);
            fire(now,"fill",RouletteFillSequence.slot(winner,i),RouletteTimeline.FILL_START+i*RouletteTimeline.FILL_STEP,emit);
        }
        fire(now,"result",winner,RouletteTimeline.STOP_END,emit);
        fire(now,"finish",winner,RouletteTimeline.FILL_END,emit);
        fire(now,"outro",winner,RouletteTimeline.HOLD_END,emit);
        double from=Math.max(previous,RouletteTimeline.SPIN_START);
        double to=Math.min(now,RouletteTimeline.STOP_END);
        if(to>from) {
            double a=RouletteTimeline.rotationDegrees(from,winner), b=RouletteTimeline.rotationDegrees(to,winner);
            // Los bordes cruzan las 9:00 cuando el disco alcanza 270 + n*45 grados.
            long first=(long)Math.floor((a-270)/45)+1, last=(long)Math.floor((b-270)/45);
            for(long edge=first;edge<=last;edge++) {
                double lo=from,hi=to,target=270+edge*45;
                for(int j=0;j<24;j++){double mid=(lo+hi)/2;if(RouletteTimeline.rotationDegrees(mid,winner)<target)lo=mid;else hi=mid;}
                double crossing=(lo+hi)/2;
                if(crossing-lastClick>=2) {
                    emit.accept(new Event("tick",RouletteTimeline.slotUnderPointer(target+.001),crossing));
                    lastClick=crossing;
                }
            }
        }
        previous=now;
    }
    private void fire(double now,String name,int slot,double at,Consumer<Event> emit) {
        if(previous<at && now>=at)emit.accept(new Event(name,slot,at));
    }
}
