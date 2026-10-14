package pe.apuyawar.systems.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import pe.apuyawar.systems.roulette.RoulettePalette;
import pe.apuyawar.systems.roulette.RouletteTimeline;

/** Arte aprobado, interpolación continua y luz localizada; el puntero nunca gira ni vibra. */
public final class RouletteRenderer {
    private static final int TEXTURE_SIZE = 512;
    /** Diámetro real del arte de la rueda dentro de la textura 512x512; única fuente de verdad
     * para dónde empieza/termina el aro dorado (usado también por RouletteLighting). */
    static final float WHEEL_DIAMETER = 458f;
    private static final float SCREEN_HEIGHT_FRACTION = 0.45f;
    private static final Identifier[] SECTORS = new Identifier[8];
    private static final Identifier RING = RouletteDraw.texture("ring");
    private static final Identifier CENTER = RouletteDraw.texture("medallion");
    private static final Identifier POINTER = RouletteDraw.texture("pointer");
    static {
        for(int i=0;i<SECTORS.length;i++)SECTORS[i]=RouletteDraw.texture("sector_"+RoulettePalette.NAMES[i]);
    }
    private RouletteRenderer() {}
    public static void render(DrawContext context,float tickDelta) {
        if(!RouletteClientState.isRunning())return;
        double tick=RouletteClientState.time(tickDelta);
        int winner=RouletteClientState.winner();
        if(winner<0||winner>=8)return;
        float opacity=(float)RouletteTimeline.alpha(tick),scale=(float)RouletteTimeline.scale(tick);
        if(opacity<=0||scale<=0)return;
        float desired=context.getScaledWindowHeight()*SCREEN_HEIGHT_FRACTION*TEXTURE_SIZE/WHEEL_DIAMETER;
        float side=Math.min(desired,Math.max(1,Math.min(context.getScaledWindowWidth(),context.getScaledWindowHeight())-18)*512f/640f);
        // Vacía cualquier dibujo anterior del HUD antes del dibujo inmediato personalizado.
        context.draw();
        boolean wasBlend=GL11.glIsEnabled(GL11.GL_BLEND),wasDepth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        int srcRgb=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dstRgb=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        float[] shaderColor=RenderSystem.getShaderColor().clone();
        var previousShader=RenderSystem.getShader();
        MatrixStack matrices=context.getMatrices();matrices.push();
        try {
            RenderSystem.disableDepthTest();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            matrices.translate(context.getScaledWindowWidth()/2f,context.getScaledWindowHeight()/2f,150);
            matrices.scale(side/TEXTURE_SIZE*scale,side/TEXTURE_SIZE*scale,1);
            double rotation=RouletteTimeline.rotationDegrees(tick,winner);
            double energy=RouletteTimeline.energy(tick,winner);
            matrices.push();
            try {
                double shake=RouletteTimeline.shake(tick);
                matrices.translate(shake*(3.2*Math.sin(tick*1.8)+.9*Math.sin(tick*4.1)),
                        shake*(2.4*Math.cos(tick*2.3)+.6*Math.sin(tick*3.6)),0);
                double visual=RouletteTimeline.visualAngle(tick);
                // Dos muestras de estela discretas debajo del arte nítido; desaparecen al frenar.
                if(energy>.4) {
                    disc(context,tick,winner,RouletteTimeline.rotationDegrees(tick-.28,winner)+visual,opacity*.045f*(float)energy,false);
                    disc(context,tick,winner,RouletteTimeline.rotationDegrees(tick-.14,winner)+visual,opacity*.065f*(float)energy,false);
                }
                disc(context,tick,winner,rotation+visual,opacity,true);
            } finally { matrices.pop(); }
            // El arte queda encolado por DrawContext; se vuelca a pantalla antes del halo
            // dibujado con Tessellator crudo para que el brillo no termine pintado debajo.
            context.draw();
            RouletteLighting.energy(context,tick,winner,rotation,opacity);
            context.draw();
            float pointerOpacity=opacity*(float)RouletteTimeline.pointerAlpha(tick);
            RouletteDraw.layer(context,POINTER,0,1,0xFFFFFF,pointerOpacity,false);
            RouletteLighting.gem(context,tick,winner,pointerOpacity);
        } finally {
            matrices.pop();
            RenderSystem.setShaderColor(shaderColor[0],shaderColor[1],shaderColor[2],shaderColor[3]);
            RenderSystem.blendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);
            if(wasBlend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            if(wasDepth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(previousShader!=null)RenderSystem.setShader(()->previousShader);
        }
    }
    private static void disc(DrawContext context,double tick,int winner,double rotation,float opacity,boolean effects) {
        for(int slot=0;slot<8;slot++) {
            float build=(float)RouletteTimeline.buildAlpha(tick,slot);
            if(build<=0)continue;
            float fill=(float)RouletteTimeline.fillBlend(tick,slot,winner);
            float angle=(float)(rotation+RouletteTimeline.sectorRotationDegrees(slot));
            float sectorScale=sectorScale(tick,slot,winner,build);
            if(fill<1||slot==winner)RouletteDraw.layer(context,SECTORS[slot],angle,sectorScale,0xFFFFFF,opacity*build,false);
            if(fill>0&&slot!=winner)RouletteDraw.layer(context,SECTORS[winner],angle,sectorScale,0xFFFFFF,opacity*build*fill,false);
        }
        float frameOpacity=opacity*(float)RouletteTimeline.frameAlpha(tick);
        RouletteDraw.layer(context,RING,(float)rotation,1,0xFFFFFF,frameOpacity,false);
        RouletteDraw.layer(context,CENTER,(float)rotation,1,0xFFFFFF,frameOpacity,false);
        // La luz por sector va después del aro dorado para que no quede tapada.
        for(int slot=0;slot<8;slot++) {
            float build=(float)RouletteTimeline.buildAlpha(tick,slot);
            if(build<=0)continue;
            float angle=(float)(rotation+RouletteTimeline.sectorRotationDegrees(slot));
            float sectorScale=sectorScale(tick,slot,winner,build);
            if(effects) {
                double entry=RouletteTimeline.buildPulse(tick,slot),conversion=RouletteTimeline.fillPulse(tick,slot,winner);
                float strength=(float)(.45*entry+.55*conversion+.46*RouletteTimeline.celebration(tick)+RouletteTimeline.energy(tick,winner)*(.035+.10*Math.pow(Math.max(0,Math.cos(Math.toRadians(angle+67.5)+tick*.12)),6)));
                // El "LED" del ícono sigue el mismo progreso 0..1 por sector (fillBlend)
                // que ya transforma la textura física, en vez de saltar los ocho a la vez
                // apenas empieza cualquier sector a convertirse.
                double fillT=RouletteTimeline.fillBlend(tick,slot,winner);
                int color=RouletteLighting.lerpColor(RoulettePalette.RGB[slot],RoulettePalette.RGB[winner],fillT);
                RouletteLighting.sector(context,angle,color,strength*opacity*build,sectorScale);
            }
        }
        if(effects)RouletteLighting.spinRim(context,tick,winner,(float)rotation,frameOpacity);
        if(effects)RouletteLighting.rim(context,(float)rotation,winner,
                frameOpacity*(float)(.25*RouletteTimeline.celebration(tick)+.12*RouletteTimeline.exitPulse(tick)));
    }
    /** Escala del sector: igual para el arte y para su brillo, con un pequeño "pop"
     * (rebote) justo en el instante en que ESE sector termina su transformación —
     * usa el mismo fillPulse que ya sincroniza el resto de los efectos, sin un
     * temporizador aparte. */
    private static float sectorScale(double tick,int slot,int winner,float build) {
        double pop=RouletteTimeline.fillPulse(tick,slot,winner);
        return (float)(.86f+.14f*build+.10*pop);
    }
}
