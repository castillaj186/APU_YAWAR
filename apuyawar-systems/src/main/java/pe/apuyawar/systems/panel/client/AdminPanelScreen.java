package pe.apuyawar.systems.panel.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import pe.apuyawar.systems.network.AdminAnnouncePayload;
import pe.apuyawar.systems.network.AdminGiveSolPePayload;
import pe.apuyawar.systems.network.AdminRouletteForcePayload;
import pe.apuyawar.systems.network.AdminTeleportToPayload;
import pe.apuyawar.systems.roulette.RoulettePalette;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Panel administrativo (Staff en Creative). Dibujado por completo desde Java —
 * la textura {@code panel.png} es del panel de supervivencia de 4 botones y no se
 * fuerza aquí. Cada acción reenvía un paquete C2S que el servidor vuelve a validar
 * (Staff + Creative) antes de ejecutar nada real.
 */
public final class AdminPanelScreen extends Screen {
    private enum Section { JUGADORES, ANUNCIOS, RULETA, ECONOMIA, EVENTOS, STAFF, HERRAMIENTAS }
    private Section section = Section.JUGADORES;
    private TextFieldWidget announceField;
    private TextFieldWidget economyTargetField;
    private TextFieldWidget economyAmountField;
    private final Map<Section, List<ClickableWidget>> sectionWidgets = new EnumMap<>(Section.class);

    public AdminPanelScreen() { super(Text.literal("APU YAWAR - Panel Administrativo")); }

    @Override protected void init() {
        for (Section s : Section.values()) sectionWidgets.put(s, new ArrayList<>());
        int y = 24;
        for (Section s : Section.values()) {
            addDrawableChild(ButtonWidget.builder(Text.literal(label(s)), b -> switchSection(s))
                    .dimensions(10, y, 120, 18).build());
            y += 20;
        }

        announceField = new TextFieldWidget(this.textRenderer, 140, 50, 260, 16, Text.literal("Mensaje"));
        addSection(Section.ANUNCIOS, announceField);
        addSection(Section.ANUNCIOS, ButtonWidget.builder(Text.literal("Enviar anuncio"), b -> sendAnnounce())
                .dimensions(140, 72, 120, 18).build());

        economyTargetField = new TextFieldWidget(this.textRenderer, 140, 50, 140, 16, Text.literal("Jugador"));
        addSection(Section.ECONOMIA, economyTargetField);
        economyAmountField = new TextFieldWidget(this.textRenderer, 290, 50, 60, 16, Text.literal("Cantidad"));
        economyAmountField.setText("1");
        addSection(Section.ECONOMIA, economyAmountField);
        addSection(Section.ECONOMIA, ButtonWidget.builder(Text.literal("Dar Sol PE"), b -> sendGiveSolPe())
                .dimensions(140, 72, 120, 18).build());

        int rx = 140;
        for (int i = 0; i < RoulettePalette.NAMES.length; i++) {
            int color = i;
            addSection(Section.RULETA, ButtonWidget.builder(Text.literal(capitalize(RoulettePalette.NAMES[i])),
                    b -> sendRouletteForce(color)).dimensions(rx + (i % 4) * 95, 50 + (i / 4) * 20, 90, 18).build());
        }

        int ty = 50;
        if (this.client != null && this.client.getNetworkHandler() != null) {
            for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
                String name = entry.getProfile().getName();
                addSection(Section.JUGADORES, ButtonWidget.builder(Text.literal("Ir a " + name), b -> sendTeleportTo(name))
                        .dimensions(140, ty, 150, 18).build());
                ty += 20;
            }
        }
        updateVisibility();
    }

    private void addSection(Section s, ClickableWidget widget) {
        addDrawableChild(widget);
        sectionWidgets.get(s).add(widget);
    }

    private void switchSection(Section s) { section = s; updateVisibility(); }
    private void updateVisibility() {
        for (var entry : sectionWidgets.entrySet()) {
            boolean visible = entry.getKey() == section;
            for (ClickableWidget widget : entry.getValue()) widget.visible = visible;
        }
    }
    private String label(Section s) {
        return switch (s) {
            case JUGADORES -> "Jugadores";
            case ANUNCIOS -> "Anuncios";
            case RULETA -> "Control de Ruleta";
            case ECONOMIA -> "Economía (Sol PE)";
            case EVENTOS -> "Eventos y Desafíos";
            case STAFF -> "Staff";
            case HERRAMIENTAS -> "Herramientas";
        };
    }
    private String capitalize(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    private void sendAnnounce() {
        String text = announceField.getText();
        if (text == null || text.isBlank()) return;
        if (ClientPlayNetworking.canSend(AdminAnnouncePayload.ID)) ClientPlayNetworking.send(new AdminAnnouncePayload(text));
        announceField.setText("");
    }
    private void sendGiveSolPe() {
        String target = economyTargetField.getText();
        if (target == null || target.isBlank()) return;
        int amount;
        try { amount = Integer.parseInt(economyAmountField.getText().trim()); } catch (NumberFormatException e) { amount = 1; }
        if (ClientPlayNetworking.canSend(AdminGiveSolPePayload.ID))
            ClientPlayNetworking.send(new AdminGiveSolPePayload(target, amount));
    }
    private void sendRouletteForce(int color) {
        if (ClientPlayNetworking.canSend(AdminRouletteForcePayload.ID))
            ClientPlayNetworking.send(new AdminRouletteForcePayload(color));
    }
    private void sendTeleportTo(String name) {
        if (ClientPlayNetworking.canSend(AdminTeleportToPayload.ID))
            ClientPlayNetworking.send(new AdminTeleportToPayload(name));
    }

    /** {@code Screen.render()} vuelve a llamar a {@code renderBackground()}
     * internamente antes de pintar los widgets (botones de sección, campos de
     * texto); eso aplicaba una segunda pasada de blur sobre el panel ya
     * dibujado. El fondo real de esta pantalla es el {@code fill()} oscuro de
     * {@link #render}, así que se sobrescribe como no-op para bloquear esa
     * llamada oculta. */
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC000000);
        ViewportFix.restoreAfterBackground(this.client);
        context.fill(0, 0, 135, this.height, 0xC0151008);
        context.drawText(this.textRenderer, "Panel Administrativo", 10, 8, 0xFFFFD700, true);
        super.render(context, mouseX, mouseY, delta);
        int ry = 36;
        switch (section) {
            case JUGADORES -> context.drawText(this.textRenderer, "Jugadores conectados: usa los botones de la derecha para teletransportarte.", 140, ry, 0xFFFFFFFF, false);
            case ANUNCIOS -> context.drawText(this.textRenderer, "Escribe el mensaje y envíalo a todo el servidor por chat.", 140, ry, 0xFFFFFFFF, false);
            case RULETA -> context.drawText(this.textRenderer, "Forzar un color (reutiliza la lógica real de la ruleta, no un simulacro).", 140, ry, 0xFFFFFFFF, false);
            case ECONOMIA -> context.drawText(this.textRenderer, "Entrega Sol PE real a un jugador conectado.", 140, ry, 0xFFFFFFFF, false);
            case EVENTOS -> context.drawText(this.textRenderer, "No disponible: usa /apuyawar reto y /apuyawar dificultad (ya existentes); falta un panel visual dedicado.", 140, ry, 0xFFAAAAAA, false);
            case STAFF -> context.drawText(this.textRenderer, "No disponible en el panel todavía: usa /apuyawar staff lista e /info <jugador>.", 140, ry, 0xFFAAAAAA, false);
            case HERRAMIENTAS -> context.drawText(this.textRenderer, "No disponible: no hay todavía un catálogo de herramientas administrativas adicionales definido.", 140, ry, 0xFFAAAAAA, false);
        }
    }

    @Override public boolean shouldPause() { return false; }
}
