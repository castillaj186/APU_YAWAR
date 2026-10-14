package pe.apuyawar.systems.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.LoggerFactory;
import pe.apuyawar.systems.network.RouletteStartPayload;
import pe.apuyawar.systems.network.IntiStartPayload;
import pe.apuyawar.systems.network.PanelOpenResponsePayload;
import pe.apuyawar.systems.network.MissionsSyncPayload;
import pe.apuyawar.systems.client.judgment.IntiClientState;
import pe.apuyawar.systems.client.judgment.IntiRenderer;
import pe.apuyawar.systems.panel.client.PanelKeyBindings;
import pe.apuyawar.systems.panel.client.SurvivalPanelScreen;
import pe.apuyawar.systems.panel.client.AdminPanelScreen;
import pe.apuyawar.systems.panel.client.MissionsScreen;

public final class ApuYawarClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        PanelKeyBindings.initialize();
        ClientPlayNetworking.registerGlobalReceiver(RouletteStartPayload.ID, (payload, context) ->
            context.client().execute(() -> RouletteClientState.begin(payload.colorIndex(), payload.sequence())));
        ClientPlayNetworking.registerGlobalReceiver(IntiStartPayload.ID, (payload, context) ->
            context.client().execute(() -> IntiClientState.begin(payload.sequence(), payload.previewOnly())));
        ClientPlayNetworking.registerGlobalReceiver(PanelOpenResponsePayload.ID, (payload, context) ->
            context.client().execute(() -> context.client().setScreen(
                    payload.adminPanel() ? new AdminPanelScreen() : new SurvivalPanelScreen())));
        ClientPlayNetworking.registerGlobalReceiver(MissionsSyncPayload.ID, (payload, context) ->
            context.client().execute(() -> {
                if (context.client().currentScreen instanceof MissionsScreen screen)
                    screen.applySync(payload.progress(), payload.rewarded());
            }));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RouletteClientState.tick(client); IntiClientState.tick(client); PanelKeyBindings.tick(client);
        });
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            float delta=tickCounter.getTickDelta(true);
            try {
                RouletteRenderer.render(drawContext, delta);
            } catch (Throwable t) {
                LoggerFactory.getLogger("apuyawar-roulette").error("Fallo en RouletteRenderer.render", t);
            }
            IntiRenderer.render(drawContext, delta);
        });
    }
}
