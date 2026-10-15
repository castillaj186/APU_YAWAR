package pe.apuyawar.momentocuy.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.RenderLayer;
import pe.apuyawar.momentocuy.block.MomentoCuyBlocks;
import pe.apuyawar.momentocuy.entity.MomentoCuyEntities;

public final class MomentoCuyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Integracion opcional: si apuyawar-lighting no esta presente (p.ej.
        // una instalacion manual que quito el jar-in-jar), el arbol sigue
        // funcionando con su luz Vanilla normal, sin RGB.
        if (FabricLoader.getInstance().isModLoaded("apuyawar_lighting")) {
            MomentoCuyLightingBridge.register();
        }
        // El Cuy Jardinero dibuja su propio modelo a mano desde el .bbmodel
        // (ver CuyModel/CuyRenderer) -- no usa EntityModelLayerRegistry
        // porque no es un ModelPart/TexturedModelData vanilla, igual que en
        // el mod antiguo (su CuyRenderer tampoco lo registraba).
        EntityRendererRegistry.register(MomentoCuyEntities.CUY_JARDINERO, CuyRenderer::new);

        for (var entry : MomentoCuyBlocks.nonSolidBlocks().entrySet()) {
            RenderLayer layer = entry.getValue() == MomentoCuyBlocks.Layer.CUTOUT ? RenderLayer.getCutout() : RenderLayer.getTranslucent();
            BlockRenderLayerMap.INSTANCE.putBlock(entry.getKey(), layer);
        }
    }
}
