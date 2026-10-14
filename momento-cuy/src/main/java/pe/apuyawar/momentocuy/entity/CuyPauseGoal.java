package pe.apuyawar.momentocuy.entity;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.Goal.Control;

import java.util.EnumSet;

/** Adaptado sin cambios de comportamiento del {@code CuyPauseGoal} del mod
 * antiguo (ver {@code CuyJardineroEntity} para el proceso de recuperación). */
public final class CuyPauseGoal extends Goal {
    private final CuyJardineroEntity cuy;

    public CuyPauseGoal(CuyJardineroEntity cuy) {
        this.cuy = cuy;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() { return this.cuy.isCuyBusy() && !this.cuy.getCuyFleeing(); }

    @Override
    public boolean shouldContinue() { return this.cuy.isCuyBusy() && !this.cuy.getCuyFleeing(); }

    @Override
    public void start() { this.cuy.getNavigation().stop(); }

    @Override
    public void tick() { this.cuy.getNavigation().stop(); }

    @Override
    public boolean shouldRunEveryTick() { return true; }
}
