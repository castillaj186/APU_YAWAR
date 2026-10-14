package pe.apuyawar.systems.events;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import pe.apuyawar.systems.challenges.ChallengeEngine;
import pe.apuyawar.systems.config.JudgmentConfig;
import pe.apuyawar.systems.difficulty.DifficultyEngine;
import pe.apuyawar.systems.judgment.IntiPresentationTimeline;
import pe.apuyawar.systems.network.IntiStartPayload;

/** Owns server event state. Client acknowledgements only sequence presentations. */
public final class JudgmentService {
    private enum PendingPhase { ROULETTE, INTI }
    private static final Map<MinecraftServer, JudgmentService> INSTANCES = new IdentityHashMap<>();
    public final ChallengeEngine challenges;
    public final DifficultyEngine difficulties;
    public final JudgmentConfig config;
    private final MinecraftServer server;
    private long clock, sequence;
    private int pendingColor = -1;
    private PendingPhase phase;
    private EventCatalog.Challenge preparedChallenge;
    private List<UUID> recipients = List.of();
    private final RouletteCompletionGate rouletteCompletion = new RouletteCompletionGate();
    private final PresentationCompletionGate intiCompletion = new PresentationCompletionGate(
        IntiPresentationTimeline.DURATION_TICKS, IntiPresentationTimeline.COMPLETION_GRACE_TICKS);

    private JudgmentService(MinecraftServer server) {
        this.server = server; config = JudgmentConfig.load();
        challenges = new ChallengeEngine(server, config); difficulties = new DifficultyEngine(server, config);
    }
    public static JudgmentService get(MinecraftServer server) { return INSTANCES.computeIfAbsent(server, JudgmentService::new); }
    public static void shutdown(MinecraftServer server) {
        var service = INSTANCES.remove(server);
        if (service != null) { service.clearPending(); service.challenges.stop(); service.difficulties.stop(); }
    }
    public boolean busy(int color) { return pendingColor >= 0 || (color == 0 && challenges.active()) || (color == 7 && difficulties.active()); }
    public long queue(int color, Collection<UUID> clientRecipients) {
        sequence++; pendingColor = color; phase = PendingPhase.ROULETTE; recipients = List.copyOf(clientRecipients);
        preparedChallenge = color == 0 ? chooseChallenge() : null;
        rouletteCompletion.begin(sequence, clock, recipients); return sequence;
    }
    public void acknowledge(ServerPlayerEntity player, long id) {
        if (pendingColor >= 0 && phase == PendingPhase.ROULETTE) rouletteCompletion.acknowledge(player.getUuid(), id);
    }
    public void acknowledgeInti(ServerPlayerEntity player, long id) {
        if (pendingColor == 0 && phase == PendingPhase.INTI) intiCompletion.acknowledge(player.getUuid(), id);
    }
    public void tick() {
        clock++; challenges.tick(); difficulties.tick();
        if (pendingColor < 0) return;
        List<UUID> online = server.getPlayerManager().getPlayerList().stream().map(ServerPlayerEntity::getUuid).toList();
        if (phase == PendingPhase.ROULETTE) {
            var status = rouletteCompletion.status(clock, online);
            if (status == RouletteCompletionGate.Status.WAITING) return;
            if (status == RouletteCompletionGate.Status.TIMED_OUT) { cancelPresentation("Tirada cancelada: no se confirmó el final de la ruleta."); return; }
            if (pendingColor == 0 && config.redEnabled && !challenges.active() && preparedChallenge != null) {
                phase = PendingPhase.INTI; intiCompletion.begin(sequence, clock, recipients);
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (recipients.contains(player.getUuid()) && ServerPlayNetworking.canSend(player, IntiStartPayload.ID)) {
                        ServerPlayNetworking.send(player, new IntiStartPayload(sequence, false));
                    }
                }
                return;
            }
            finishRouletteResult(); return;
        }
        var status = intiCompletion.status(clock, online);
        if (status == PresentationCompletionGate.Status.WAITING) return;
        if (status == PresentationCompletionGate.Status.TIMED_OUT) {
            server.getPlayerManager().broadcast(Text.literal("APU YAWAR | La presentación de Inti agotó la espera; el juicio comienza."), false);
        }
        EventCatalog.Challenge selected = preparedChallenge;
        clearPending();
        if (selected != null && config.redEnabled && !challenges.active()) challenges.start(selected, server.getPlayerManager().getPlayerList());
    }
    private void finishRouletteResult() {
        int color = pendingColor; clearPending();
        if (color == 7 && config.orangeEnabled && !difficulties.active()) {
            var choices = EventCatalog.DIFFICULTIES.values().stream().filter(d -> !config.disabledDifficulties.contains(d.id()) && (config.includeDatapackVariants || d.legacy() <= 35)).toList();
            if (!choices.isEmpty()) difficulties.start(choices.get(server.getOverworld().random.nextInt(choices.size())));
        }
    }
    private EventCatalog.Challenge chooseChallenge() {
        var choices = EventCatalog.CHALLENGES.values().stream().filter(c -> !config.disabledChallenges.contains(c.id()) && (config.includeDatapackVariants || !c.id().startsWith("juicio_"))).toList();
        return choices.isEmpty() ? null : choices.get(server.getOverworld().random.nextInt(choices.size()));
    }
    private void cancelPresentation(String message) { clearPending(); server.getPlayerManager().broadcast(Text.literal("APU YAWAR | " + message), false); }
    private void clearPending() { pendingColor = -1; phase = null; preparedChallenge = null; recipients = List.of(); }
    public boolean pending() { return pendingColor >= 0; }
    public void cancelPending() { clearPending(); }
    public void playIntiPreview(Collection<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) if (ServerPlayNetworking.canSend(player, IntiStartPayload.ID)) ServerPlayNetworking.send(player, new IntiStartPayload(0L, true));
    }
    public void loaded(Entity entity) {
        if (entity.getCommandTags().contains(ChallengeEngine.HAZARD_TAG) && !challenges.owns(entity) && !difficulties.owns(entity)) { entity.discard(); return; }
        difficulties.loaded(entity); challenges.loaded(entity);
        if (entity instanceof net.minecraft.entity.LivingEntity living) ChallengeEngine.removeHeart(living);
    }
}
