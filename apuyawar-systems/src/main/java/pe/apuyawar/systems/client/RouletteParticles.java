package pe.apuyawar.systems.client;

import java.util.ArrayList;
import java.util.List;
import pe.apuyawar.systems.roulette.*;

/** Partículas locales analíticas: sin asignaciones por fotograma, máximo 80. */
public final class RouletteParticles {
    public record Particle(double x,double y,double vx,double vy,double born,double life,double size,int color) {
        public double travel(double tick) { return (1-Math.exp(-.065*Math.max(0,tick-born)))/.065; }
        public double alpha(double tick) { return Math.pow(Math.max(0,1-(tick-born)/life),2); }
    }
    private static final List<Particle> ACTIVE=new ArrayList<>();
    private RouletteParticles() {}
    public static List<Particle> active() { return ACTIVE; }
    public static void clear() { ACTIVE.clear(); }
    public static void prune(double tick) { ACTIVE.removeIf(p->tick>=p.born()+p.life()); }
    public static void emit(RouletteEvents.Event e,int winner) {
        int count=switch(e.name()){case "appear"->2;case "fill"->5;case "finish"->28;case "tick"->1;default->0;};
        if(e.name().equals("tick") && ((int)e.tick())%6!=0)return;
        double rotation=RouletteTimeline.rotationDegrees(e.tick(),winner);
        for(int i=0;i<count&&ACTIVE.size()<80;i++) {
            double angle=e.name().equals("finish")?i*360.0/count:rotation-67.5+e.slot()*45+(i-count/2.0)*5;
            double a=Math.toRadians(angle),radius=e.name().equals("fill")?146:229;
            double speed=.6+(i%4)*.35;
            int color=RoulettePalette.RGB[e.name().equals("appear")||e.name().equals("tick")?e.slot():winner];
            ACTIVE.add(new Particle(Math.cos(a)*radius,Math.sin(a)*radius,Math.cos(a)*speed,Math.sin(a)*speed,e.tick(),14+i%5*2,2+i%3,color));
        }
    }
}
