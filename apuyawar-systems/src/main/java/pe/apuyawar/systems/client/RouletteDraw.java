package pe.apuyawar.systems.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL11;

/** Dibujo inmediato con composición alfa explícita; ninguna textura depende del estado del HUD. */
final class RouletteDraw {
    private RouletteDraw() {}
    static Identifier texture(String name) {
        return Identifier.of("apuyawar_systems","textures/gui/roulette/"+name+".png");
    }
    static void layer(DrawContext context, Identifier id, float rotation, float scale,
                      int rgb, float opacity, boolean light) {
        float alpha=Math.max(0,Math.min(1,opacity));
        if(alpha<=0)return;
        MatrixStack matrices=context.getMatrices();matrices.push();
        try {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
            matrices.scale(scale,scale,1);
            RenderSystem.enableBlend();
            if(light)RenderSystem.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
            else RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,alpha);
            context.drawTexture(id,-256,-256,0,0,512,512,512,512);
        } finally {
            RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.defaultBlendFunc();matrices.pop();
        }
    }
}
