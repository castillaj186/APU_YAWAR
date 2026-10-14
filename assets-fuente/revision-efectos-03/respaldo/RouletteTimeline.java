package pe.apuyawar.systems.roulette;

/** Coreografía local en ticks; 620 ticks = 31 segundos nominales. */
public final class RouletteTimeline {
    public static final int SECTOR_COUNT = 8;
    public static final int BUILD_START = 8;
    public static final int BUILD_STEP = 9;
    public static final int BUILD_FADE = 6;
    public static final int SPIN_START = 84;
    public static final int ACCEL_END = 132;
    public static final int CRUISE_END = 252;
    public static final int STOP_END = 332;
    public static final int SETTLE_END = 350;
    public static final int FILL_START = 360;
    public static final int FILL_STEP = 20;
    public static final int FILL_FADE = 12;
    public static final int FILL_END = FILL_START + SECTOR_COUNT * FILL_STEP;
    public static final int HOLD_END = 560;
    public static final int DURATION = 620;
    private static final double TOP_SPEED = 20.0;
    private RouletteTimeline() {}
    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
    private static double ease(double value) {
        double t = clamp(value, 0, 1);
        return t*t*t*(t*(t*6-15)+10);
    }
    private static double positiveModulo(double a, double b) { return ((a % b) + b) % b; }
    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SECTOR_COUNT) throw new IllegalArgumentException("Posición fuera de 0..7");
    }
    public static double progress(double tick) { return clamp(tick / DURATION, 0, 1); }
    /** Todos los maestros ocupan inicialmente [-90°, -45°]. */
    public static double sectorRotationDegrees(int slot) { checkSlot(slot); return slot * 45.0; }
    public static double targetAngle(int winner) {
        return positiveModulo(180 - (-67.5 + sectorRotationDegrees(winner)), 360);
    }
    private static double angleAtAccelEnd() { return TOP_SPEED * (ACCEL_END - SPIN_START) / 2.0; }
    private static double angleAtCruiseEnd() { return angleAtAccelEnd() + TOP_SPEED * (CRUISE_END - ACCEL_END); }
    private static double finalAngle(int winner) {
        double ideal = angleAtCruiseEnd() + TOP_SPEED * (STOP_END - CRUISE_END) / 2.0;
        double target = targetAngle(winner);
        return target + Math.round((ideal - target) / 360.0) * 360.0;
    }
    public static double rotationDegrees(double tick, int winner) {
        double t = clamp(tick, 0, DURATION);
        if (t < SPIN_START) return -12 * (1 - ease(t / 32));
        if (t <= ACCEL_END) {
            double duration = ACCEL_END - SPIN_START;
            double u = (t - SPIN_START) / duration;
            // Integral de velocidad cosenoidal: empieza en cero y llega a TOP_SPEED.
            return TOP_SPEED * duration / 2 * (u - Math.sin(Math.PI*u) / Math.PI);
        }
        if (t <= CRUISE_END) return angleAtAccelEnd() + (t - ACCEL_END) * TOP_SPEED;
        double finish = finalAngle(winner);
        if (t <= STOP_END) {
            double duration = STOP_END - CRUISE_END;
            double u = (t - CRUISE_END) / duration;
            double h00 = 2*u*u*u - 3*u*u + 1;
            double h10 = u*u*u - 2*u*u + u;
            double h01 = -2*u*u*u + 3*u*u;
            return h00*angleAtCruiseEnd() + h10*(TOP_SPEED*duration) + h01*finish;
        }
        if (t < SETTLE_END) {
            double u = (t - STOP_END) / (SETTLE_END - STOP_END);
            double envelope = Math.sin(Math.PI*u);
            return finish + 3*Math.sin(2*Math.PI*u)*envelope*envelope;
        }
        return finish;
    }
    public static double buildAlpha(double tick, int slot) {
        checkSlot(slot); return ease((tick - BUILD_START - slot*BUILD_STEP) / BUILD_FADE);
    }
    public static int visibleSectors(double tick) {
        int count = 0;
        for (int slot = 0; slot < SECTOR_COUNT; slot++) if (buildAlpha(tick, slot) > 0) count++;
        return count;
    }
    /** Una transición por posición en sentido horario, sin cambiar geometría. */
    public static double fillBlend(double tick, int slot) {
        checkSlot(slot); return ease((tick - FILL_START - slot*FILL_STEP) / FILL_FADE);
    }
    public static double fillProgress(double tick) { return clamp((tick - FILL_START) / (FILL_END - FILL_START), 0, 1); }
    public static double frameAlpha(double tick) { return ease(tick / 30); }
    public static double pointerAlpha(double tick) { return ease((tick - 12) / 28); }
    public static double scale(double tick) {
        if (tick < 32) return .30 + .70*ease(tick/32);
        if (tick <= HOLD_END) return 1;
        return 1 - .96*ease((tick - HOLD_END) / (DURATION - HOLD_END));
    }
    public static double alpha(double tick) {
        if (tick < 14) return ease(tick/14);
        if (tick <= HOLD_END) return 1;
        return 1 - ease((tick - HOLD_END) / (DURATION - HOLD_END));
    }
    public static double flash(double tick) {
        if (tick >= 78 && tick <= 94) return .35*Math.sin(Math.PI*(tick-78)/16);
        if (tick >= FILL_END && tick <= FILL_END+30) return .70*Math.sin(Math.PI*(tick-FILL_END)/30);
        if (tick >= HOLD_END && tick <= HOLD_END+24) return .30*Math.sin(Math.PI*(tick-HOLD_END)/24);
        return 0;
    }
    public static double resultTint(double tick) { return fillProgress(tick); }
    public static double effectScale(double tick) {
        if (tick > HOLD_END) return 1 + .12*ease((tick-HOLD_END)/(DURATION-HOLD_END));
        return 1 + .015*Math.sin(tick*.15)*fillProgress(tick);
    }
    public static double glowStrength(double tick) { return .14 + fillProgress(tick)*(.07 + .03*Math.sin(tick*.15)); }
    public static double shake(double tick) {
        if (tick >= ACCEL_END && tick < CRUISE_END) return .35;
        if (tick >= CRUISE_END && tick < STOP_END) return .35*(1-ease((tick-CRUISE_END)/(STOP_END-CRUISE_END)));
        return 0;
    }
}