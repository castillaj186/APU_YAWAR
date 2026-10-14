package pe.apuyawar.systems.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import pe.apuyawar.systems.network.RouletteStartPayload;
import pe.apuyawar.systems.network.IntiStartPayload;
import pe.apuyawar.systems.client.judgment.IntiClientState;
import pe.apuyawar.systems.client.judgment.IntiRenderer;

public final class ApuYawarClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RouletteStartPayload.ID, (payload, context) ->
            context.client().execute(() -> RouletteClientState.begin(payload.colorIndex(), payload.sequence())));
        ClientPlayNetworking.registerGlobalReceiver(IntiStartPayload.ID, (payload, context) ->
            context.client().execute(() -> IntiClientState.begin(payload.sequence(), payload.previewOnly())));
        ClientTickEvents.END_CLIENT_TICK.register(client -> { RouletteClientState.tick(client); IntiClientState.tick(client); });
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            float delta=tickCounter.getTickDelta(true);
            RouletteRenderer.render(drawContext, delta);
            IntiRenderer.render(drawContext, delta);
        });
    }
}
