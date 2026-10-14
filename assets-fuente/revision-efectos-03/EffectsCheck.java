import pe.apuyawar.systems.roulette.RouletteTimeline;
import pe.apuyawar.systems.roulette.RoulettePalette;
import pe.apuyawar.systems.client.RouletteGem;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Locale;
import java.util.HashSet;

/** Pruebas de funciones de producción y exportación de cuadros a 30 fps. */
public final class EffectsCheck {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static int delta(int a,int b){int m=0;for(int s=0;s<=16;s+=8)m=Math.max(m,Math.abs(((a>>s)&255)-((b>>s)&255)));return m;}
    public static void main(String[] args)throws Exception {
        var positions=new HashSet<Double>();
        for(int slot=0;slot<8;slot++) {
            positions.add(RouletteTimeline.sectorRotationDegrees(slot));
            check(RouletteTimeline.buildAlpha(RouletteTimeline.SPIN_START,slot)==1,"Construcción incompleta");
            check(RouletteTimeline.fillBlend(RouletteTimeline.FILL_END,slot)==1,"Llenado incompleto");
            check(RouletteTimeline.buildPulse(RouletteTimeline.BUILD_START+slot*RouletteTimeline.BUILD_STEP+3.5,slot)>.99,"Flash de entrada ausente");
            check(RouletteTimeline.fillPulse(RouletteTimeline.FILL_START+slot*RouletteTimeline.FILL_STEP+4,slot)>.99,"Flash de conversión ausente");
            double a=RouletteTimeline.targetAngle(slot);
            check(RouletteTimeline.slotUnderPointer(a)==slot,"Casilla bajo puntero incorrecta");
            check(RouletteGem.sampleColor(a)==RoulettePalette.RGB[slot],"Gema no reconoce centro del color");
        }
        check(positions.size()==8,"Posiciones duplicadas");
        for(int winner=0;winner<8;winner++) {
            double lastSpeed=Double.POSITIVE_INFINITY;
            for(double t=0;t<=RouletteTimeline.DURATION;t+=.1) {
                check(Double.isFinite(RouletteTimeline.rotationDegrees(t,winner)),"Ángulo inválido");
                check(RouletteTimeline.alpha(t)>=0&&RouletteTimeline.alpha(t)<=1,"Alfa fuera de rango");
                check(RouletteGem.overlayStrength(t,winner)>=0&&RouletteGem.overlayStrength(t,winner)<=1,"Iluminación de gema fuera de rango");
                check(Math.abs(RouletteTimeline.visualAngle(t))<.3,"Vibración angular excesiva");
                if(t>=RouletteTimeline.CRUISE_END&&t<RouletteTimeline.STOP_END) {
                    double speed=RouletteTimeline.angularVelocity(t,winner);
                    check(speed>=-.001&&speed<=lastSpeed+.001,"Frenado no progresivo");lastSpeed=speed;
                }
                if(t>=RouletteTimeline.SETTLE_END) {
                    check(RouletteTimeline.shake(t)==0&&RouletteTimeline.visualAngle(t)==0,"Vibración altera resultado");
                    check(RouletteTimeline.slotUnderPointer(RouletteTimeline.rotationDegrees(t,winner))==winner,"Parada desalineada");
                    check(RouletteGem.color(t,winner)==RoulettePalette.RGB[winner],"Gema no mantiene ganador");
                }
            }
            for(int boundary:new int[]{32,54,150,204,212}) {
                check(Math.abs(RouletteTimeline.rotationDegrees(boundary+.0001,winner)-RouletteTimeline.rotationDegrees(boundary-.0001,winner))<.02,"Salto de ángulo");
                check(Math.abs(RouletteTimeline.angularVelocity(boundary-.005,winner)-RouletteTimeline.angularVelocity(boundary+.005,winner))<.10,"Salto de velocidad");
            }
            for(int slot=0;slot<8;slot++) {
                double before=0;for(double t=0;t<=310;t+=.25){double fill=RouletteTimeline.fillBlend(t,slot);check(fill>=before,"Llenado retrocede");before=fill;}
            }
        }
        for(double rotation=0;rotation<360;rotation+=45)
            check(delta(RouletteGem.sampleColor(rotation-.001),RouletteGem.sampleColor(rotation+.001))<=1,"Color de gema salta en límite");
        check(RouletteTimeline.alpha(0)==0&&RouletteTimeline.alpha(310)==0,"Residuo al desaparecer");
        var csv=new StringBuilder("tick,winner,rotation,scale,alpha,frame,pointer,energy,celebration,exit,gem,gemStrength,shake,visual");
        for(String prefix:new String[]{"build","fill","entryPulse","fillPulse"})for(int s=0;s<8;s++)csv.append(",").append(prefix).append(s);csv.append(",gemOverlay\n");
        for(int winner=0;winner<8;winner++)for(int f=0;f<=465;f++) {
            double t=f*20.0/30;
            csv.append(String.format(Locale.ROOT,"%.8f,%d,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%d,%.8f,%.8f,%.8f",t,winner,RouletteTimeline.rotationDegrees(t,winner),RouletteTimeline.scale(t),RouletteTimeline.alpha(t),RouletteTimeline.frameAlpha(t),RouletteTimeline.pointerAlpha(t),RouletteTimeline.energy(t,winner),RouletteTimeline.celebration(t),RouletteTimeline.exitPulse(t),RouletteGem.color(t,winner),RouletteTimeline.gemStrength(t,winner),RouletteTimeline.shake(t),RouletteTimeline.visualAngle(t)));
            for(int s=0;s<8;s++)csv.append(String.format(Locale.ROOT,",%.8f",RouletteTimeline.buildAlpha(t,s)));
            for(int s=0;s<8;s++)csv.append(String.format(Locale.ROOT,",%.8f",RouletteTimeline.fillBlend(t,s)));
            for(int s=0;s<8;s++)csv.append(String.format(Locale.ROOT,",%.8f",RouletteTimeline.buildPulse(t,s)));
            for(int s=0;s<8;s++)csv.append(String.format(Locale.ROOT,",%.8f",RouletteTimeline.fillPulse(t,s)));
            csv.append(String.format(Locale.ROOT,",%.8f\n",RouletteGem.overlayStrength(t,winner)));
        }
        Files.writeString(Path.of(args[0]),csv);
        System.out.println("OK: 8 ganadores, 8 posiciones, construcción/llenado/flash, frenado continuo, vibración acotada y anulada, gema dinámica continua y correcta, salida sin residuo. 310 ticks / 15.5 s.");
    }
}
