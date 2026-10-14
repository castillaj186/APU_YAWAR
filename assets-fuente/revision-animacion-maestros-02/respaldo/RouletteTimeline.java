package pe.apuyawar.systems.roulette;

/**
 * Coreografía determinista del giro, calculada por tiempo continuo en TICKS.
 * La punta fija está a las 9:00; los sectores se numeran desde las 12:00.
 * 375 ticks = ~18.75 segundos nominales a 20 TPS.
 */
public final class RouletteTimeline {
    public static final int DURATION = 375;
    private RouletteTimeline() {}
    private static double clamp(double v, double a, double b) { return Math.max(a, Math.min(b, v)); }
    private static double smoother(double t) { t = clamp(t,0,1); return t*t*(3-2*t); }
    public static double progress(double tick) { return clamp(tick / DURATION, 0, 1); }

    /** Sector 0 = rojo, centro inicial -67.5 grados; apuntador a 180 grados. */
    public static double targetAngle(int winner) {
        double sectorCenter = -90 + 22.5 + winner * 45.0;
        return positiveModulo(180 - sectorCenter, 360);
    }
    private static double positiveModulo(double a, double b) { return ((a % b) + b) % b; }

    public static double rotationDegrees(double tick, int winner) {
        double t = clamp(tick, 0, DURATION);
        if (t < 19) return -20 * (1 - smoother(t / 19));
        if (t <= 64) {
            double u = (t - 19) / 45.0;
            return 600.0 * u*u;   // aceleración creciente
        }
        double at64 = 600.0;
        if (t <= 225) return at64 + (t - 64) * 40.0;
        double at225 = at64 + (225 - 64) * 40.0;
        // Distancia de frenado estimada y alineación EXACTA al ganador.
        double ideal = at225 + 1300.0;
        double finish = targetAngle(winner) + Math.round((ideal-targetAngle(winner))/360.0)*360.0;
        if (t <= 290) {
            double u = (t - 225) / 65.0;
            double h00 = 2*u*u*u - 3*u*u + 1;
            double h10 = u*u*u - 2*u*u + u;
            double h01 = -2*u*u*u + 3*u*u;
            return h00*at225 + h10*(40.0*65) + h01*finish;
        }
        if (t <= 308) {
            double u = (t - 290)/18.0;
            return finish + 7.0 * Math.sin(Math.PI*u*3) * (1-u)*(1-u);
        }
        return finish;
    }

    public static double scale(double tick) {
        if (tick < 19) return 0.15 + 0.85*smoother(tick/19);
        if (tick <= 337) return 1.0;
        return 1.0 - .96*smoother((tick-337)/38.0);
    }
    public static double alpha(double tick) {
        if (tick < 4) return smoother(tick/4);
        if (tick <= 337) return 1.0;
        return 1.0-smoother((tick-337)/38.0);
    }
    public static double flash(double tick) {
        if (tick>=14 && tick <=19) return Math.sin(Math.PI*(tick-14)/5)*.75;
        if (tick>=307 && tick<=333) return Math.sin(Math.PI*(tick-307)/26)*.95;
        return 0;
    }
    public static double shake(double tick) {
        if (tick >=65 && tick<225) return .75;
        if (tick >=288 && tick <=326) return 3.0 * (1 - (tick-288)/50.0);
        return 0;
    }
    public static int visibleSectors(double tick) {
        if (tick>=14) return 8;
        return (int) clamp(Math.floor(tick*8.0/14.0)+1, 0,8);
    }
}
