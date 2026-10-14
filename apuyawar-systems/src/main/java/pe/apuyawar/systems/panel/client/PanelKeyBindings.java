package pe.apuyawar.systems.panel.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import pe.apuyawar.systems.network.PanelOpenRequestPayload;

/** Tecla K: pide al servidor qué panel corresponde; el cliente nunca decide esto solo. */
public final class PanelKeyBindings {
    private static KeyBinding openPanel;
    private PanelKeyBindings() {}
    public static void initialize() {
        openPanel = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.apuyawar_systems.panel",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "key.categories.apuyawar_systems"));
    }
    public static void tick(MinecraftClient client) {
        if (openPanel == null) return;
        while (openPanel.wasPressed()) {
            if (client.player != null && client.currentScreen == null
                    && ClientPlayNetworking.canSend(PanelOpenRequestPayload.ID)) {
                ClientPlayNetworking.send(new PanelOpenRequestPayload());
            }
        }
    }
}
