package pe.apuyawar.lighting.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import pe.apuyawar.lighting.config.LightingConfig;

/**
 * "Estrella celestial" (sección 4.2.C del encargo V4): poco frecuente,
 * breve destello con resplandor turquesa/dorado, usando la secuencia de
 * sprites animada {@code minecraft:glitter_0..7} que ya usa el End Rod
 * de vanilla (forma de chispa/estrella de 4 puntas, pixel art real, no
 * dibujada a mano esta ronda). Crece al aparecer y se apaga
 * progresivamente (sección 4.2.C: "breve destello... desaparición
 * progresiva"), nunca una estrella gigante (tamaño máximo acotado).
 */
final class CelestialStarParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final float maxScale;

    private CelestialStarParticle(ClientWorld world, double x, double y, double z,
                                    SpriteProvider spriteProvider, int rgb) {
        super(world, x, y, z, 0, 0, 0);
        // V4.1, entrega 3: ver documentación equivalente en
        // CelestialSparkParticle -- mismos tres controles, leídos en
        // vivo al nacer.
        LightingConfig config = ApuYawarLightingClient.config();
        float sizeScale = Math.max(0.1f, config.celestialParticleSizeMultiplier);
        float lifeScale = Math.max(0.1f, config.celestialParticleLifetimeMultiplier);
        float speedScale = Math.max(0.0f, config.celestialParticleSpeedMultiplier);

        this.spriteProvider = spriteProvider;
        this.setSpriteForAge(spriteProvider);
        this.red = ((rgb >> 16) & 0xFF) / 255f;
        this.green = ((rgb >> 8) & 0xFF) / 255f;
        this.blue = (rgb & 0xFF) / 255f;
        this.alpha = 1.0f;
        this.scale = 0.0f;
        this.maxScale = 0.22f * sizeScale;
        this.maxAge = Math.max(4, Math.round((16 + this.random.nextInt(10)) * lifeScale));
        this.gravityStrength = 0f;
        this.collidesWithWorld = false;
        this.velocityX = (this.random.nextDouble() - 0.5) * 0.004 * speedScale;
        this.velocityY = this.random.nextDouble() * 0.006 * speedScale;
        this.velocityZ = (this.random.nextDouble() - 0.5) * 0.004 * speedScale;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.setSpriteForAge(spriteProvider);
        float progress = this.age / (float) this.maxAge;
        // Crece rápido al nacer (0 -> tamaño máximo pequeño), se apaga
        // en la segunda mitad de su vida -- "breve destello", no una
        // estrella que se queda flotando grande.
        float grow = Math.min(1f, progress / 0.25f);
        float fade = progress < 0.5f ? 1f : clamp01(1f - (progress - 0.5f) / 0.5f);
        this.scale = maxScale * grow;
        this.alpha = fade;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_LIT;
    }

    static final class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;
        private final int rgb;

        Factory(SpriteProvider spriteProvider, int rgb) {
            this.spriteProvider = spriteProvider;
            this.rgb = rgb;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world,
                                        double x, double y, double z,
                                        double velocityX, double velocityY, double velocityZ) {
            return new CelestialStarParticle(world, x, y, z, spriteProvider, rgb);
        }
    }
}
