import pe.apuyawar.systems.roulette.RouletteTimeline;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;

/** Prueba contra la clase real de producción, sin depender de Minecraft ni JUnit. */
public final class TimelineCheck {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static double speed(double t, int winner) {
        return (RouletteTimeline.rotationDegrees(t+.001,winner)-RouletteTimeline.rotationDegrees(t-.001,winner))/.002;
    }
    public static void main(String[] args) throws Exception {
        var positions = new HashSet<Double>();
        for (int slot=0;slot<8;slot++) {
            positions.add(RouletteTimeline.sectorRotationDegrees(slot));
            require(RouletteTimeline.buildAlpha(RouletteTimeline.SPIN_START,slot)==1,"Construcción incompleta");
            require(RouletteTimeline.fillBlend(RouletteTimeline.FILL_START,slot)==0,"Llenado prematuro");
            require(RouletteTimeline.fillBlend(RouletteTimeline.FILL_END,slot)==1,"Llenado final incompleto");
            double build=0,fill=0;
            for(double t=0;t<=RouletteTimeline.DURATION;t+=.25) {
                double b=RouletteTimeline.buildAlpha(t,slot),f=RouletteTimeline.fillBlend(t,slot);
                require(b>=build&&f>=fill,"Transición retrocede"); build=b;fill=f;
            }
        }
        require(positions.size()==8,"Orientación duplicada");
        for(int winner=0;winner<8;winner++) {
            double previousSpeed=Double.POSITIVE_INFINITY;
            for(double t=RouletteTimeline.CRUISE_END+.01;t<RouletteTimeline.STOP_END;t+=.1) {
                double v=speed(t,winner);
                require(v>=-.001&&v<=previousSpeed+.001,"Frenado no progresivo: "+winner);
                previousSpeed=v;
            }
            for(int boundary:new int[]{RouletteTimeline.SPIN_START,RouletteTimeline.ACCEL_END,RouletteTimeline.CRUISE_END,RouletteTimeline.STOP_END,RouletteTimeline.SETTLE_END}) {
                require(Math.abs(RouletteTimeline.rotationDegrees(boundary+.0001,winner)-RouletteTimeline.rotationDegrees(boundary-.0001,winner))<.01,"Salto angular");
                require(Math.abs(speed(boundary-.01,winner)-speed(boundary+.01,winner))<.05,"Salto de velocidad");
            }
            double end=RouletteTimeline.rotationDegrees(RouletteTimeline.FILL_START,winner);
            double pointer=((-67.5+winner*45+end)%360+360)%360;
            require(Math.abs(pointer-180)<.001,"Ganador fuera del puntero");
            for(double t=RouletteTimeline.FILL_START;t<=RouletteTimeline.HOLD_END;t+=.25)
                require(Math.abs(RouletteTimeline.rotationDegrees(t,winner)-end)<.001,"Disco se mueve durante llenado");
        }
        require(RouletteTimeline.alpha(0)==0&&RouletteTimeline.alpha(RouletteTimeline.DURATION)==0,"Entrada/salida brusca");
        require(RouletteTimeline.visibleSectors(0)==0,"Sectores aparecen de golpe");
        require(RouletteTimeline.FILL_END<RouletteTimeline.HOLD_END,"Sin permanencia del resultado");
        var csv=new StringBuilder("tick,winner,rotation,scale,alpha,frame,pointer,flash,glow,effectScale,tint");
        for(int i=0;i<8;i++)csv.append(",build").append(i);
        for(int i=0;i<8;i++)csv.append(",fill").append(i);
        csv.append('\n');
        for(int winner:new int[]{0,5,3,6})for(int tick:new int[]{0,32,84,200,330,360,400,440,480,520,550,590}) {
            csv.append(String.format(Locale.ROOT,"%d,%d,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f",tick,winner,RouletteTimeline.rotationDegrees(tick,winner),RouletteTimeline.scale(tick),RouletteTimeline.alpha(tick),RouletteTimeline.frameAlpha(tick),RouletteTimeline.pointerAlpha(tick),RouletteTimeline.flash(tick),RouletteTimeline.glowStrength(tick),RouletteTimeline.effectScale(tick),RouletteTimeline.resultTint(tick)));
            for(int i=0;i<8;i++)csv.append(String.format(Locale.ROOT,",%.8f",RouletteTimeline.buildAlpha(tick,i)));
            for(int i=0;i<8;i++)csv.append(String.format(Locale.ROOT,",%.8f",RouletteTimeline.fillBlend(tick,i)));
            csv.append('\n');
        }
        Files.writeString(Path.of(args[0]),csv);
        System.out.println("OK: 8 posiciones únicas, 8 ganadores alineados, construcción y llenado monotónicos, frenado progresivo, continuidad angular/velocidad, permanencia y salida. Duración: "+RouletteTimeline.DURATION+" ticks.");
    }
}
