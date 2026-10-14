package pe.apuyawar.momentocuy.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import pe.apuyawar.momentocuy.entity.CuyJardineroEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Adaptado del {@code CuyRenderer} del mod antiguo (ver
 * {@code CuyJardineroEntity} para el proceso completo de recuperación):
 * decide qué animación de {@link CuyModel} reproducir según el movimiento
 * real (parado/caminando/corriendo a cuatro patas/saltando/cayendo/
 * aterrizando) y según la acción autónoma actual del Cuy
 * ({@code getCuyAction()}/{@code getCuyActionSerial()}). Simplificado
 * respecto al original: aquella clase atendía a 11 criaturas distintas
 * (bípedo/cuadrúpedo genérico); como este mod SOLO recupera al Cuy Jardinero,
 * ya no hace falta el chequeo {@code instanceof} ni la lógica genérica de
 * "nutria" — es directamente un renderer de {@code CuyJardineroEntity}.
 */
public final class CuyRenderer extends EntityRenderer<CuyJardineroEntity> {
    private static final Identifier TEXTURE = Identifier.of("momento_cuy", "textures/entity/cuy_jardinero.png");
    private final CuyModel model;
    private final Map<CuyJardineroEntity, MotionState> states = new WeakHashMap<>();

    public CuyRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new CuyModel();
        this.shadowRadius = 0.3F;
    }

    @Override
    public Identifier getTexture(CuyJardineroEntity entity) { return TEXTURE; }

    @Override
    public void render(CuyJardineroEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        MotionState state = this.states.computeIfAbsent(entity, e -> new MotionState());
        float seconds = (entity.age + tickDelta) / 20.0F;
        Vec3d velocity = entity.getVelocity();
        double horizontalSpeed = Math.hypot(velocity.x, velocity.z);
        boolean grounded = entity.isOnGround();
        boolean fleeing = entity.getCuyFleeing();
        boolean wantsQuad = horizontalSpeed > 0.11 || (fleeing && horizontalSpeed > 0.025);

        if (!grounded && state.wasGrounded) state.airAt = seconds;
        if (grounded && !state.wasGrounded) state.landedAt = seconds;
        state.wasGrounded = grounded;
        if (grounded && wantsQuad != state.quadruped) {
            state.quadruped = wantsQuad;
            state.transition = wantsQuad ? "transition_to_quad" : "transition_to_biped";
            state.transitionAt = seconds;
        }

        String animation;
        float animationTime = seconds;
        if (!grounded) {
            animation = velocity.y > 0.03 ? (state.quadruped ? "run_jump" : "jump_biped") : "fall";
            animationTime = seconds - state.airAt;
        } else if (seconds - state.landedAt < 0.45F) {
            animation = "land";
            animationTime = seconds - state.landedAt;
        } else if (state.transition != null && seconds - state.transitionAt < ("transition_to_quad".equals(state.transition) ? 0.45F : 0.48F)) {
            animation = state.transition;
            animationTime = seconds - state.transitionAt;
        } else if (state.quadruped) {
            animation = "run_quadruped";
        } else if (horizontalSpeed > 0.012) {
            animation = "walk_biped";
        } else {
            animation = "idle_biped";
        }

        int serial = entity.getCuyActionSerial();
        String commanded = entity.getCuyAction();
        if (serial != state.lastActionSerial) {
            state.lastActionSerial = serial;
            if (commanded != null && !commanded.isEmpty()) {
                state.specialAction = commanded;
                state.specialStartedAt = seconds;
            }
        }
        if (commanded == null || commanded.isEmpty()) state.specialAction = null;
        if (state.specialAction != null) {
            animation = state.specialAction;
            animationTime = Math.max(0.0F, seconds - state.specialStartedAt);
        }

        this.model.setHurtOverlay(entity.hurtTime > 0 || entity.deathTime > 0);
        this.model.renderAnimated(matrices, buffer, light, animation, animationTime);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private static final class MotionState {
        boolean quadruped = false;
        boolean wasGrounded = true;
        String transition = null;
        float transitionAt = 0.0F;
        float airAt = 0.0F;
        float landedAt = -100.0F;
        int lastActionSerial = -1;
        String specialAction = null;
        float specialStartedAt = 0.0F;
    }
}
