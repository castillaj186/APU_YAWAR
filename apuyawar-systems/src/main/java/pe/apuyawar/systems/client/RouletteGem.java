package pe.apuyawar.systems.client;

import pe.apuyawar.systems.roulette.RoulettePalette;
import pe.apuyawar.systems.roulette.RouletteTimeline;

/** Muestreo temporal determinista: independiente del FPS y del ganador comunicado. */
public final class RouletteGem {
    private RouletteGem() {}
    public static double overlayStrength(double tick,int winner) {
        double activation=Math.max(0,Math.min(1,(tick-RouletteTimeline.SPIN_START)/8));
        activation=activation*activation*(3-2*activation);
        return activation*(.80+.12*RouletteTimeline.energy(tick,winner)+.08*RouletteTimeline.celebration(tick));
    }
    public static int color(double tick,int winner) {
        if(tick<RouletteTimeline.SPIN_START)return 0xFFFFFF;
        if(tick>=RouletteTimeline.STOP_END)return RoulettePalette.RGB[winner];
        // Ventana corta de 0,12 s: evita parpadeos duros cuando pasan varios sectores por segundo.
        double red=0,green=0,blue=0,weights=0;
        for(int sample=0;sample<6;sample++) {
            double time=Math.max(0,tick-sample*.48),weight=6-sample;
            int rgb=sampleColor(RouletteTimeline.rotationDegrees(time,winner));
            red+=((rgb>>16)&255)*weight;green+=((rgb>>8)&255)*weight;blue+=(rgb&255)*weight;weights+=weight;
        }
        return ((int)Math.round(red/weights)<<16)|((int)Math.round(green/weights)<<8)|(int)Math.round(blue/weights);
    }
    public static int sampleColor(double rotation) {
        double angle=((270-rotation)%360+360)%360;
        int slot=RouletteTimeline.slotUnderPointer(rotation),neighbor=slot;
        double fraction=angle/45-slot,blend=0;
        if(fraction<.12){neighbor=(slot+7)%8;blend=.5*(1-smooth(fraction/.12));}
        else if(fraction>.88){neighbor=(slot+1)%8;blend=.5*smooth((fraction-.88)/.12);}
        int from=RoulettePalette.RGB[slot],to=RoulettePalette.RGB[neighbor],result=0;
        for(int shift=0;shift<=16;shift+=8)
            result|=(int)Math.round(((from>>shift)&255)*(1-blend)+((to>>shift)&255)*blend)<<shift;
        return result;
    }
    private static double smooth(double t) { return t*t*(3-2*t); }
}
