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
        // V4.3, fase 4 (sección 6.1 del encargo: "comprueba que
        // tamanoparticulas=2.00 no produzca estrellas exageradamente
        // grandes") -- la estrella es la más llamativa de las tres
        // partículas (destello + secuencia animada), así que su
        // multiplicador de tamaño se acota aparte del de la chispa
        // (que puede seguir creciendo sin tope -- un halo grande sigue
        // leyéndose como "suave", nunca como "gigante"). 1.8 con la
        // escala base de 0.22 da 0.40 como máximo absoluto, nunca el
        // "¿0.44 con el multiplicador en 2.0?" que se habría obtenido
        // sin este tope.
        float sizeScale = Math.min(1.8f, Math.max(0.1f, config.celestialParticleSizeMultiplier));
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
