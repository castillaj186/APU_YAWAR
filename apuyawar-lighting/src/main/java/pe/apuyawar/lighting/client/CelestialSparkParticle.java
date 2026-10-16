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
 * "Chispa celestial" (sección 4.2.B del encargo V4): núcleo brillante +
 * halo tenue, reutilizando el sprite {@code minecraft:glow} de vanilla
 * (el mismo que usa el calamar brillante -- suave, ya tiene núcleo+halo
 * integrados en un solo sprite, no se dibuja nada nuevo a mano).
 * Movimiento flotante propio (deriva + oscilación lenta, sin gravedad),
 * no el de ningún calamar -- ver {@link #tick()}.
 */
final class CelestialSparkParticle extends SpriteBillboardParticle {
    private final double driftPhase;

    private final double speedScale;

    private CelestialSparkParticle(ClientWorld world, double x, double y, double z,
                                     double velocityX, double velocityY, double velocityZ,
                                     SpriteProvider spriteProvider, int rgb) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        // V4.1, entrega 3 (sección 6.5 del encargo: "verifica que todos
        // los controles modifiquen realmente el comportamiento en
        // ejecución") -- leídos en vivo al nacer la partícula, no sólo
        // guardados en el config sin usar.
        LightingConfig config = ApuYawarLightingClient.config();
        float sizeScale = Math.max(0.1f, config.celestialParticleSizeMultiplier);
        float lifeScale = Math.max(0.1f, config.celestialParticleLifetimeMultiplier);
        this.speedScale = Math.max(0.0, config.celestialParticleSpeedMultiplier);

        this.setSprite(spriteProvider);
        this.red = ((rgb >> 16) & 0xFF) / 255f;
        this.green = ((rgb >> 8) & 0xFF) / 255f;
        this.blue = (rgb & 0xFF) / 255f;
        this.alpha = 0.85f;
        this.scale = (0.12f + this.random.nextFloat() * 0.08f) * sizeScale;
        this.maxAge = Math.max(4, Math.round((40 + this.random.nextInt(40)) * lifeScale));
        this.gravityStrength = 0f;
        this.collidesWithWorld = false;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.driftPhase = this.random.nextDouble() * Math.PI * 2;
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
        // Oscilación suave en vez de caída/vuelo recto -- sección 4.4 del
        // encargo: "no quiero partículas cayendo rápidamente", "oscilación
        // suave", "no todas en la misma dirección" (cada chispa tiene su
        // propia fase, fijada al nacer).
        double wobble = Math.sin((this.age * 0.08) + this.driftPhase) * 0.0025 * speedScale;
        this.move((this.velocityX + wobble) * speedScale, this.velocityY * 0.4 * speedScale, (this.velocityZ - wobble) * speedScale);
        this.velocityY *= 0.98;
        float progress = this.age / (float) this.maxAge;
        this.alpha = 0.85f * (progress < 0.15f ? progress / 0.15f : clamp01(1f - (progress - 0.6f) / 0.4f));
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    @Override
    public ParticleTextureSheet getType() {
        // Sección 4.5 del encargo: "renderizado fullbright" -- esta hoja
        // ignora el mapa de luz del mundo, no hace falta oscurecer la
        // chispa de día ni calcular ningún brillo manualmente.
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
            return new CelestialSparkParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider, rgb);
        }
    }
}
