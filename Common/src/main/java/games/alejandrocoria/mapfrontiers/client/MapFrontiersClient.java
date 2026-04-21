package games.alejandrocoria.mapfrontiers.client;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.MapFrontiersAPIBootstrap;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.frontier.ClientFrontierEvents;
import games.alejandrocoria.mapfrontiers.client.frontier.ClientFrontierOperationService;
import games.alejandrocoria.mapfrontiers.client.frontier.ClientFrontierRuntime;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontierLocalOverrides;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUD;
import games.alejandrocoria.mapfrontiers.client.gui.screen.page.ModSettingsPage;
import games.alejandrocoria.mapfrontiers.client.settings.ClientSettingsProfileEvents;
import games.alejandrocoria.mapfrontiers.common.api.MapFrontiersApiLogAdapter;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandshake;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class MapFrontiersClient {
    private enum HandshakeSignal {
        ACK("handshake acknowledgment"),
        SETTINGS_PROFILE("settings profile"),
        TIMEOUT("timeout");

        private final String displayName;

        HandshakeSignal(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private static final long HANDSHAKE_TIMEOUT_MS = 1800L;
    private static final long HANDSHAKE_RETRY_MS = 600L;

    private static IClientAPI jmAPI;
    private static final ClientConnectionState connectionState = new ClientConnectionState();
    private static ClientFrontierRuntime frontierRuntime;
    private static ModSettingsPage.Tab lastSettingsTab = ModSettingsPage.Tab.Credits;

    protected static KeyMapping openSettingsKey;
    private static HUD hud;

    private static @Nullable BlockPos lastPlayerPosition = null;
    private static @Nullable ResourceKey<Level> lastPlayerDimension = null;
    private static boolean frontierActivationDirty = true;
    private static long hudActiveFrontiersRevision = 0L;
    private static final List<FrontierOverlay> hudActiveFrontiers = new ArrayList<>();
    private static final Set<UUID> hudActiveFrontierIds = new HashSet<>();
    private static final Map<UUID, FrontierOverlay> announcementActiveFrontiers = new HashMap<>();
    private static long lastTitleTime;

    private static FrontierData clipboard = null;
    private static ClientLevel lastClientLevel = null;

    protected static void init() {
        MapFrontiersAPIBootstrap.setLogger(new MapFrontiersApiLogAdapter());
        ClientConfig.initialize();

        ClientGlobalEvents.subscribeClientTickEvent(MapFrontiersClient.class, MapFrontiersClient::handleClientTick);
        ClientGlobalEvents.subscribePlayerTickEvent(MapFrontiersClient.class, MapFrontiersClient::handlePlayerTick);
        ClientGlobalEvents.subscribeHudRenderEvent(MapFrontiersClient.class, MapFrontiersClient::handleHudRender);
        ClientGlobalEvents.subscribeClientConnectedEvent(MapFrontiersClient.class, MapFrontiersClient::handleClientConnected);
        ClientGlobalEvents.subscribeClientDisconnectedEvent(MapFrontiersClient.class, MapFrontiersClient::handleClientDisconnected);
        ClientGlobalEvents.subscribeUpdatedConfigEvent(MapFrontiersClient.class, MapFrontiersClient::markFrontierActivationDirty);
    }

    private static void handleClientTick(Minecraft client) {
        if (client.level == null) {
            return;
        }

        if (!isJourneyMapPluginAvailable()) {
            return;
        }

        handleWorldChange(client);
        processHandshake();
        updateOverlayManagers();
        tickHud();
    }

    private static void handlePlayerTick(Minecraft client, @Nullable Player player) {
        if (client.level == null) {
            return;
        }

        handleOpenSettingsKey();

        if (!isJourneyMapPluginAvailable()) {
            return;
        }

        if (player == null || ClientConfig.FRONTIER_VISIBILITY.get() == ClientConfig.Visibility.Never) {
            clearFrontierActivationState();
            return;
        }

        updateFrontierActivationState(client, player);
    }

    private static void handleWorldChange(Minecraft client) {
        if (client.level != lastClientLevel) {
            clearFrontierActivationState();
            if (connectionState.restartHandshakeIfUnresolved()) {
                if (lastClientLevel != null) {
                    MapFrontiers.LOGGER.info("World changed before handshake resolution, restarting handshake.");
                }
            }
            lastClientLevel = client.level;
        }
    }

    private static void updateOverlayManagers() {
        ClientFrontierRuntime runtime = requireFrontierRuntime();
        FrontiersOverlayManager frontiersOverlayManager = runtime.getGlobalFrontiersOverlayManager();
        FrontiersOverlayManager personalFrontiersOverlayManager = runtime.getPersonalFrontiersOverlayManager();
        frontiersOverlayManager.updateAllOverlays(false);
        personalFrontiersOverlayManager.updateAllOverlays(false);
    }

    private static void tickHud() {
        if (hud != null) {
            hud.tick();
        }
    }

    private static void handleOpenSettingsKey() {
        while (openSettingsKey != null && openSettingsKey.consumeClick()) {
            new ModSettingsPage(false).display();
        }
    }

    private static void updateFrontierActivationState(Minecraft client, Player player) {
        BlockPos currentPlayerPosition = player.blockPosition();
        ResourceKey<Level> currentPlayerDimension = player.level().dimension();

        if (!frontierActivationDirty
                && currentPlayerPosition.equals(lastPlayerPosition)
                && currentPlayerDimension.equals(lastPlayerDimension)) {
            return;
        }

        lastPlayerPosition = currentPlayerPosition;
        lastPlayerDimension = currentPlayerDimension;
        frontierActivationDirty = false;

        updateHudActiveFrontiers(currentPlayerDimension, currentPlayerPosition);
        handleFrontierAnnouncements(client, player, currentPlayerDimension, currentPlayerPosition);
    }

    private static void handleFrontierAnnouncements(Minecraft client, Player player, ResourceKey<Level> dimension, BlockPos playerPosition) {
        Map<UUID, FrontierOverlay> currentlyActiveFrontiers = collectAnnouncementActiveFrontiers(dimension, playerPosition);

        for (FrontierOverlay frontier : announcementActiveFrontiers.values()) {
            if (!currentlyActiveFrontiers.containsKey(frontier.getId())) {
                boolean frontierAnnounceInChat = frontier.getVisibility(FrontierData.VisibilityData.Visibility.AnnounceInChat);
                if (ClientConfig.getVisibilityValue(ClientConfig.ANNOUNCE_IN_CHAT.get(), frontierAnnounceInChat)
                        && (frontier.isNamed() || ClientConfig.ANNOUNCE_UNNAMED_FRONTIERS.get())) {
                    player.displayClientMessage(Component.translatable("mapfrontiers.chat.leaving", createAnnounceTextWithName(frontier)), false);
                }
            }
        }

        for (FrontierOverlay frontier : currentlyActiveFrontiers.values()) {
            if (!announcementActiveFrontiers.containsKey(frontier.getId())
                    && (frontier.isNamed() || ClientConfig.ANNOUNCE_UNNAMED_FRONTIERS.get())) {
                Component text = createAnnounceTextWithName(frontier);

                boolean frontierAnnounceInChat = frontier.getVisibility(FrontierData.VisibilityData.Visibility.AnnounceInChat);
                if (ClientConfig.getVisibilityValue(ClientConfig.ANNOUNCE_IN_CHAT.get(), frontierAnnounceInChat)) {
                    player.displayClientMessage(Component.translatable("mapfrontiers.chat.entering", text), false);
                }

                boolean frontierAnnounceInTitle = frontier.getVisibility(FrontierData.VisibilityData.Visibility.AnnounceInTitle);
                if (ClientConfig.getVisibilityValue(ClientConfig.ANNOUNCE_IN_TITLE.get(), frontierAnnounceInTitle)) {
                    if (ClientConfig.TITLE_ANNOUNCEMENT_ABOVE_HOTBAR.get()) {
                        client.gui.setOverlayMessage(text, false);
                    } else if (System.currentTimeMillis() >= lastTitleTime + ClientConfig.TITLE_ANNOUNCEMENT_TIMEOUT.get() / 20 * 1000L) {
                        lastTitleTime = System.currentTimeMillis();
                        client.gui.setTimes(10, ClientConfig.TITLE_ANNOUNCEMENT_DURATION.get(), 20);
                        client.gui.setTitle(text);
                    }
                }
            }
        }

        announcementActiveFrontiers.clear();
        announcementActiveFrontiers.putAll(currentlyActiveFrontiers);
    }

    private static void handleHudRender(GuiGraphics graphics, float delta) {
        if (!isJourneyMapPluginAvailable()) {
            return;
        }

        if (hud == null) {
            hud = new HUD();
        } else {
            hud.drawInGameHUD(graphics, delta);
        }
    }

    private static void handleClientConnected() {
        if (!isJourneyMapPluginAvailable()) {
            MapFrontiers.LOGGER.warn(
                    "JourneyMap did not initialize the MapFrontiers client plugin. World features are disabled for this session. Check mod version compatibility."
                );
            return;
        }

        ensureFrontierRuntime();
        connectionState.restartHandshake();

        MapFrontiers.LOGGER.info("Client world session started");
    }

    private static void handleClientDisconnected() {
        if (frontierRuntime != null) {
            frontierRuntime.close();
            frontierRuntime = null;
        }

        if (hud != null) {
            hud = null;
        }

        connectionState.restartHandshake();
        lastClientLevel = null;
        clearFrontierActivationState();
        MapFrontiersAPIBootstrap.clearClientAPI();

        ChatFrontiers.clear();

        MapFrontiers.LOGGER.info("Client world session ended");
    }

    private static Component createAnnounceTextWithName(FrontierOverlay frontier) {
        if (!frontier.isNamed()) {
            MutableComponent text = Component.translatable("mapfrontiers.unnamed", ChatFormatting.ITALIC);
            text.withStyle(style -> style.withItalic(true).withColor(ColorConstants.TEXT_MEDIUM));
            return text;
        }

        String name = frontier.getName1().trim();
        String name2 = frontier.getName2().trim();
        if (!StringUtils.isBlank(name2)) {
            if (!name.isEmpty()) {
                name += " ";
            }
            name += name2;
        }

        MutableComponent text = Component.literal(name);
        text.withStyle(style -> style.withColor(frontier.getColor()));
        return text;
    }

    public static void setJmAPI(IClientAPI newJmAPI) {
        jmAPI = newJmAPI;
    }

    public static boolean isJourneyMapPluginAvailable() {
        return jmAPI != null;
    }

    private static @Nullable ClientFrontierRuntime ensureFrontierRuntime() {
        if (jmAPI == null) {
            return null;
        }

        if (frontierRuntime == null) {
            frontierRuntime = new ClientFrontierRuntime(jmAPI);
        }

        frontierRuntime.ensureInitialized();
        return frontierRuntime;
    }

    private static ClientFrontierRuntime requireFrontierRuntime() {
        ClientFrontierRuntime runtime = ensureFrontierRuntime();
        if (runtime == null) {
            throw new IllegalStateException("JourneyMap plugin is not available.");
        }

        return runtime;
    }

    private static FrontiersOverlayManager getFrontiersOverlayManagerOrNull(boolean personal) {
        ClientFrontierRuntime runtime = ensureFrontierRuntime();
        if (runtime == null) {
            return null;
        }

        if (personal) {
            return runtime.getPersonalFrontiersOverlayManager();
        }

        return runtime.getGlobalFrontiersOverlayManager();
    }

    public static void setFrontiersFromServer(List<FrontierData> globalFrontiers, List<FrontierData> personalFrontiers) {
        ClientFrontierRuntime runtime = ensureFrontierRuntime();
        if (runtime == null) {
            return;
        }

        if (!runtime.hasInitializedManagers()) {
            return;
        }

        MapFrontiers.LOGGER.debug("Received initial frontier snapshot from server. global={}, personal={}",
                globalFrontiers.size(), personalFrontiers.size());
        runtime.getSyncService().applyServerSnapshot(globalFrontiers, personalFrontiers);
        connectionState.markInitialFrontiersReceived();
        publishClientApiIfReady();
        if (hud != null) {
            hud.frontierChanged();
        }
    }

    public static List<FrontierOverlay> getFrontiers(boolean personal, ResourceKey<Level> dimension) {
        FrontiersOverlayManager manager = getFrontiersOverlayManagerOrNull(personal);
        if (manager == null) {
            return List.of();
        }

        return manager.getAllFrontiers(dimension);
    }

    public static List<FrontierOverlay> getAllFrontiers(boolean personal) {
        FrontiersOverlayManager manager = getFrontiersOverlayManagerOrNull(personal);
        if (manager == null) {
            return List.of();
        }

        return manager.getAllFrontiers().values().stream().flatMap(List::stream).toList();
    }

    public static void updateSelectedFrontierMarker(boolean personal, ResourceKey<Level> dimension, @Nullable FrontierOverlay frontier) {
        FrontiersOverlayManager manager = getFrontiersOverlayManagerOrNull(personal);
        if (manager != null) {
            manager.updateSelectedMarker(dimension, frontier);
        }
    }

    public static @Nullable FrontierOverlay getCopiedPersonalFrontier(UUID copiedFromId) {
        FrontiersOverlayManager manager = getFrontiersOverlayManagerOrNull(true);
        return manager == null ? null : manager.getFrontierCopiedFrom(copiedFromId);
    }

    public static List<FrontierOverlay> getFrontiersInPosition(ResourceKey<Level> dimension, BlockPos pos, double maxDistanceToOpen,
                                                              @Nullable Context.MapType fullscreenMapType) {
        FrontiersOverlayManager personalFrontiersOverlayManager = getFrontiersOverlayManagerOrNull(true);
        FrontiersOverlayManager frontiersOverlayManager = getFrontiersOverlayManagerOrNull(false);
        if (personalFrontiersOverlayManager == null || frontiersOverlayManager == null) {
            return List.of();
        }

        List<FrontierOverlay> frontiers = personalFrontiersOverlayManager.getFrontiersInPosition(dimension, pos, maxDistanceToOpen, fullscreenMapType);
        frontiers.addAll(frontiersOverlayManager.getFrontiersInPosition(dimension, pos, maxDistanceToOpen, fullscreenMapType));
        frontiers.sort((f1, f2) -> Float.compare(f1.area, f2.area));
        return frontiers;
    }

    public static List<FrontierOverlay> getFrontiersForHUD() {
        return List.copyOf(hudActiveFrontiers);
    }

    public static long getHudActiveFrontiersRevision() {
        return hudActiveFrontiersRevision;
    }

    public static FrontierLocalOverrides getLocalOverrides() {
        ClientFrontierRuntime runtime = requireFrontierRuntime();
        return runtime.getLocalOverrides();
    }

    public static ClientFrontierOperationService getOperationService() {
        ClientFrontierRuntime runtime = requireFrontierRuntime();
        return runtime.getOperationService();
    }

    public static ClientFrontierEvents getFrontierEvents() {
        ClientFrontierRuntime runtime = requireFrontierRuntime();
        return runtime.getFrontierEvents();
    }

    public static ClientSettingsProfileEvents getSettingsProfileEvents() {
        ClientFrontierRuntime runtime = requireFrontierRuntime();
        return runtime.getSettingsProfileEvents();
    }

    public static SettingsProfile getSettingsProfile() {
        return connectionState.getSettingsProfile();
    }

    public static void setLastSettingsTab(ModSettingsPage.Tab tab) {
        lastSettingsTab = tab;
    }

    public static ModSettingsPage.Tab getLastSettingsTab() {
        return lastSettingsTab;
    }

    public static KeyMapping.Category registerKeyMappingCategory() {
        return KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "keybinding_category"));
    }

    public static Component getOpenSettingsKey() {
        if (openSettingsKey == null || openSettingsKey.isUnbound()) {
            return null;
        } else {
            return openSettingsKey.getTranslatedKeyMessage();
        }
    }

    public static boolean isModOnServer() {
        return connectionState.isModOnServer();
    }

    public static void receiveSettingsProfile(SettingsProfile profile) {
        ClientFrontierRuntime runtime = ensureFrontierRuntime();
        if (runtime == null) {
            return;
        }

        if (!connectionState.updateSettingsProfile(profile)) {
            return;
        }

        MapFrontiers.LOGGER.debug("Received settings profile from server.");
        runtime.getSettingsProfileEvents().postUpdated(profile);
        resolveHandshake(true, HandshakeSignal.SETTINGS_PROFILE);
    }

    public static void receiveHandshakeAck(long nonce) {
        ClientConnectionState.HandshakeOutcome outcome = connectionState.onHandshakeAck(nonce);
        if (outcome == ClientConnectionState.HandshakeOutcome.RESOLVED) {
            MapFrontiers.LOGGER.debug("Received handshake acknowledgment from server.");
            publishClientApiIfReady();
            MapFrontiers.LOGGER.info("Handshake resolved from {}. mapfrontiers on server: {}",
                    HandshakeSignal.ACK.displayName(), connectionState.isModOnServer());
            return;
        }

        if (outcome == ClientConnectionState.HandshakeOutcome.UPGRADED) {
            MapFrontiers.LOGGER.debug("Received late handshake acknowledgment from server.");
            upgradeToModOnServer(HandshakeSignal.ACK);
        }
    }

    private static void processHandshake() {
        if (!isJourneyMapPluginAvailable()) {
            return;
        }

        if (connectionState.isHandshakeResolved()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (connectionState.shouldSendHandshake(now, HANDSHAKE_RETRY_MS)) {
            if (!connectionState.isHandshakeSent()) {
                MapFrontiers.LOGGER.debug("Sending initial handshake to server.");
            }

            PacketHandler.sendToServer(new PacketHandshake(connectionState.markHandshakeSent(now)));
        }

        if (connectionState.hasHandshakeTimedOut(now, HANDSHAKE_TIMEOUT_MS)) {
            MapFrontiers.LOGGER.debug("Handshake timed out after {} ms.", HANDSHAKE_TIMEOUT_MS);
            resolveHandshake(false, HandshakeSignal.TIMEOUT);
        }
    }

    private static void resolveHandshake(boolean hasModOnServer, HandshakeSignal source) {
        ClientConnectionState.HandshakeOutcome outcome = connectionState.resolveHandshake(hasModOnServer);
        if (outcome == ClientConnectionState.HandshakeOutcome.NONE) {
            return;
        }

        if (outcome == ClientConnectionState.HandshakeOutcome.UPGRADED) {
            upgradeToModOnServer(source);
            return;
        }

        publishClientApiIfReady();
        MapFrontiers.LOGGER.info("Handshake resolved from {}. mapfrontiers on server: {}",
                source.displayName(), connectionState.isModOnServer());
    }

    private static void upgradeToModOnServer(HandshakeSignal source) {
        long handshakeStartedAtMs = connectionState.getHandshakeStartedAtMs();
        long elapsedMs = handshakeStartedAtMs > 0L ? Math.max(0L, System.currentTimeMillis() - handshakeStartedAtMs) : -1L;
        if (elapsedMs >= 0L) {
            MapFrontiers.LOGGER.warn(
                    "Received {} after handshake timeout (expected {} ms, received at {} ms). Upgrading connection mode to server-mod.",
                    source.displayName(), HANDSHAKE_TIMEOUT_MS, elapsedMs
            );
        } else {
            MapFrontiers.LOGGER.warn(
                    "Received {} after handshake timeout (expected {} ms). Upgrading connection mode to server-mod.",
                    source.displayName(), HANDSHAKE_TIMEOUT_MS
            );
        }
    }

    private static void publishClientApiIfReady() {
        ClientFrontierRuntime runtime = ensureFrontierRuntime();
        if (runtime == null) {
            return;
        }

        if (!connectionState.shouldPublishClientApi()) {
            return;
        }

        MapFrontiersAPIBootstrap.setClientAPI(runtime.getOrCreateClientApi());
        connectionState.markClientApiPublished();
        MapFrontiers.LOGGER.info(
                "Published client API. modOnServer={}, initialSettingsProfileReceived={}, initialFrontiersReceived={}",
                connectionState.isModOnServer(),
                connectionState.isInitialSettingsProfileReceived(),
                connectionState.isInitialFrontiersReceived()
        );
    }

    public static void setClipboard(FrontierData newClipboard) {
        clipboard = new FrontierData(newClipboard);
    }

    public static FrontierData getClipboard() {
        return clipboard;
    }

    private static void updateHudActiveFrontiers(ResourceKey<Level> dimension, BlockPos playerPosition) {
        List<FrontierOverlay> currentlyActiveFrontiers = collectHudActiveFrontiers(dimension, playerPosition);
        boolean changed = hasActiveHudFrontiersChanged(currentlyActiveFrontiers);
        hudActiveFrontiers.clear();
        hudActiveFrontiers.addAll(currentlyActiveFrontiers);
        hudActiveFrontierIds.clear();
        for (FrontierOverlay frontier : currentlyActiveFrontiers) {
            hudActiveFrontierIds.add(frontier.getId());
        }
        if (changed) {
            ++hudActiveFrontiersRevision;
        }
    }

    private static boolean hasActiveHudFrontiersChanged(List<FrontierOverlay> currentlyActiveFrontiers) {
        if (hudActiveFrontiers.size() != currentlyActiveFrontiers.size()) {
            return true;
        }

        for (int i = 0; i < currentlyActiveFrontiers.size(); ++i) {
            if (hudActiveFrontiers.get(i) != currentlyActiveFrontiers.get(i)) {
                return true;
            }
        }

        return false;
    }

    private static List<FrontierOverlay> collectHudActiveFrontiers(ResourceKey<Level> dimension, BlockPos pos) {
        List<FrontierOverlay> frontiers = new ArrayList<>();
        appendQualifiedFrontiers(frontiers, getFrontiersOverlayManagerOrNull(true), dimension, pos, hudActiveFrontierIds, false);
        appendQualifiedFrontiers(frontiers, getFrontiersOverlayManagerOrNull(false), dimension, pos, hudActiveFrontierIds, false);
        prioritizeActiveFrontiers(frontiers);
        return frontiers;
    }

    private static Map<UUID, FrontierOverlay> collectAnnouncementActiveFrontiers(ResourceKey<Level> dimension, BlockPos pos) {
        List<FrontierOverlay> frontiers = new ArrayList<>();
        appendQualifiedFrontiers(frontiers, getFrontiersOverlayManagerOrNull(true), dimension, pos, announcementActiveFrontiers.keySet(), true);
        appendQualifiedFrontiers(frontiers, getFrontiersOverlayManagerOrNull(false), dimension, pos, announcementActiveFrontiers.keySet(), true);
        prioritizeActiveFrontiers(frontiers);

        Map<UUID, FrontierOverlay> activeFrontiers = new HashMap<>();
        for (FrontierOverlay frontier : frontiers) {
            activeFrontiers.put(frontier.getId(), frontier);
        }
        return activeFrontiers;
    }

    private static void appendQualifiedFrontiers(List<FrontierOverlay> target, @Nullable FrontiersOverlayManager manager,
                                                 ResourceKey<Level> dimension, BlockPos pos, Set<UUID> currentlyActiveFrontierIds,
                                                 boolean requireAnnouncementVisibility) {
        if (manager == null) {
            return;
        }

        for (FrontierOverlay frontier : manager.getAllFrontiers(dimension)) {
            boolean alreadyActive = currentlyActiveFrontierIds.contains(frontier.getId());
            if (qualifiesForHudOrAnnouncement(frontier, pos, alreadyActive, requireAnnouncementVisibility)) {
                target.add(frontier);
            }
        }
    }

    private static boolean qualifiesForHudOrAnnouncement(FrontierOverlay frontier, BlockPos pos, boolean alreadyActive,
                                                         boolean requireAnnouncementVisibility) {
        if (requireAnnouncementVisibility) {
            boolean announceInChat = frontier.getVisibility(FrontierData.VisibilityData.Visibility.AnnounceInChat);
            boolean announceInTitle = frontier.getVisibility(FrontierData.VisibilityData.Visibility.AnnounceInTitle);
            if (!ClientConfig.getVisibilityValue(ClientConfig.ANNOUNCE_IN_CHAT.get(), announceInChat)
                    && !ClientConfig.getVisibilityValue(ClientConfig.ANNOUNCE_IN_TITLE.get(), announceInTitle)) {
                return false;
            }
        } else if (!ClientConfig.getVisibilityValue(ClientConfig.FRONTIER_VISIBILITY.get(),
                frontier.getVisibility(FrontierData.VisibilityData.Visibility.Frontier))) {
            return false;
        }

        if (frontier.getMode() == FrontierData.Mode.Path) {
            if (frontier.getPoints().isEmpty()) {
                return false;
            }
            return frontier.pointIsInside(pos, ClientConfig.getPathActivationDistance(alreadyActive));
        }

        if (frontier.getMode() == FrontierData.Mode.Vertex && frontier.getVertices().size() < 3) {
            return false;
        }

        return frontier.pointIsInside(pos, 0.0);
    }

    private static void prioritizeActiveFrontiers(List<FrontierOverlay> frontiers) {
        boolean hasAreaFrontier = frontiers.stream().anyMatch(frontier -> frontier.getMode() != FrontierData.Mode.Path);
        if (hasAreaFrontier) {
            frontiers.removeIf(frontier -> frontier.getMode() == FrontierData.Mode.Path);
        }

        frontiers.sort(Comparator.comparingDouble(frontier -> frontier.area));
    }

    private static void clearFrontierActivationState() {
        boolean hadActiveHudFrontiers = !hudActiveFrontiers.isEmpty();
        lastPlayerPosition = null;
        lastPlayerDimension = null;
        frontierActivationDirty = true;
        hudActiveFrontiers.clear();
        hudActiveFrontierIds.clear();
        announcementActiveFrontiers.clear();
        if (hadActiveHudFrontiers) {
            ++hudActiveFrontiersRevision;
        }
    }

    public static void markFrontierActivationDirty() {
        frontierActivationDirty = true;
    }
}
