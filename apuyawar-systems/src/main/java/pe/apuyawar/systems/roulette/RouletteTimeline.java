package pe.apuyawar.systems.roulette;

/** Coreografía local en ticks; 310 ticks = 15,5 segundos nominales. */
public final class RouletteTimeline {
    public static final int SECTOR_COUNT = 8;
    public static final int BUILD_START = 2;
    public static final int BUILD_STEP = 3;
    public static final int BUILD_FADE = 4;
    public static final int SPIN_START = 32;
    public static final int ACCEL_END = 54;
    public static final int CRUISE_END = 150;
    public static final int STOP_END = 204;
    public static final int SETTLE_END = 212;
    public static final int FILL_START = 216;
    public static final int FILL_STEP = 5;
    public static final int FILL_FADE = 4;
    public static final int FILL_END = FILL_START + SECTOR_COUNT * FILL_STEP;
    public static final int HOLD_END = 278;
    public static final int DURATION = 310;
    private static final double TOP_SPEED = 54.0;
    private RouletteTimeline() {}
    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
    private static double ease(double value) {
        double t = clamp(value, 0, 1);
        return clamp(t*t*t*(t*(t*6-15)+10),0,1);
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
        if (t < SPIN_START) return -12 * (1 - ease(t / 24));
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
    public static double fillBlend(double tick, int slot, int winner) {
        checkSlot(slot); return ease((tick - RouletteFillSequence.start(winner, slot)) / FILL_FADE);
    }
    public static double fillProgress(double tick) { return clamp((tick - FILL_START) / (FILL_END - FILL_START), 0, 1); }
    public static double frameAlpha(double tick) { return ease(tick / 16); }
    public static double pointerAlpha(double tick) { return ease((tick - 4) / 16); }
    public static double scale(double tick) {
        if (tick < 24) return .30 + .70*ease(tick/24);
        if (tick <= HOLD_END) return 1;
        return 1 - .96*ease((tick - HOLD_END) / (DURATION - HOLD_END));
    }
    public static double alpha(double tick) {
        if (tick < 8) return ease(tick/8);
        if (tick <= HOLD_END) return 1;
        return 1 - ease((tick - HOLD_END) / (DURATION - HOLD_END));
    }
    /** Pulso suave con cero en ambos extremos. */
    public static double pulse(double tick, double start, double duration) {
        double u = (tick-start)/duration;
        return u <= 0 || u >= 1 ? 0 : Math.pow(Math.sin(Math.PI*u), 2);
    }
    public static double buildPulse(double tick, int slot) {
        checkSlot(slot);return pulse(tick, BUILD_START+slot*BUILD_STEP, 7);
    }
    public static double fillPulse(double tick, int slot, int winner) {
        checkSlot(slot);return pulse(tick, RouletteFillSequence.start(winner, slot), 8);
    }
    public static double celebration(double tick) { return pulse(tick, FILL_END, 22); }
    public static double exitPulse(double tick) { return pulse(tick, HOLD_END-2, 18); }
    public static double flash(double tick) {
        return .35*pulse(tick,SPIN_START-2,10)+.7*celebration(tick)+.4*exitPulse(tick);
    }
    public static double angularVelocity(double tick, int winner) {
        return (rotationDegrees(tick+.001,winner)-rotationDegrees(tick-.001,winner))/.002;
    }
    public static double energy(double tick, int winner) {
        if(tick < SPIN_START || tick >= STOP_END)return 0;
        return clamp(angularVelocity(tick,winner)/TOP_SPEED,0,1);
    }
    /** Vibración acotada que se anula al parar; no modifica el ángulo matemático. */
    public static double shake(double tick) {
        if(tick < SPIN_START || tick >= STOP_END)return 0;
        double rise=ease((tick-SPIN_START)/(ACCEL_END-SPIN_START));
        double fall=1-ease((tick-CRUISE_END)/(STOP_END-CRUISE_END));
        return .65*rise*fall;
    }
    public static double visualAngle(double tick) { return .45*shake(tick)*Math.sin(tick*2.7); }
    /** Casilla realmente bajo el puntero a las 9:00. No depende de vibración. */
    public static int slotUnderPointer(double rotation) {
        return (int)Math.floor(positiveModulo(270-rotation,360)/45);
    }
    public static double gemStrength(double tick, int winner) {
        return .22+.20*energy(tick,winner)+.28*celebration(tick)+.16*exitPulse(tick);
    }
    public static double resultTint(double tick) { return fillProgress(tick); }
    public static double effectScale(double tick) { return 1+.08*exitPulse(tick); }
    public static double glowStrength(double tick) { return .06+.07*flash(tick); }
}
