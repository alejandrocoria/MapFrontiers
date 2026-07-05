package games.alejandrocoria.mapfrontiers.client;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.MapFrontiersAPIBootstrap;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.FrontierDisplayVisibility;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUD;
import games.alejandrocoria.mapfrontiers.client.gui.screen.page.ModSettingsPage;
import games.alejandrocoria.mapfrontiers.client.settings.ClientSettingsProfileEvents;
import games.alejandrocoria.mapfrontiers.client.territory.ClientTerritoryOperationService;
import games.alejandrocoria.mapfrontiers.client.territory.ClientTerritoryRuntime;
import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientCollectionEvents;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionLocalOverrides;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionScope;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionUiStateStore;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.ClientFrontierEvents;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierLocalOverrides;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.api.MapFrontiersApiLogAdapter;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandshake;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibility;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.Context;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
    private static final float ANNOUNCEMENT_MIN_BRIGHTNESS = 0.5f;

    private static IClientAPI jmAPI;
    private static final ClientConnectionState connectionState = new ClientConnectionState();
    private static ClientTerritoryRuntime territoryRuntime;
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

    private static @Nullable FrontierData frontierClipboard = null;
    private static @Nullable CollectionData collectionClipboard = null;
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

        if (player == null || ClientConfig.FRONTIER_VISIBILITY.get() == FrontierDisplayVisibility.Never) {
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
        ClientTerritoryRuntime runtime = requireTerritoryRuntime();
        runtime.tickPersistence();
        runtime.processOverlayManagers();
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
                boolean frontierAnnounceInChat = frontier.getVisibility(FrontierVisibility.AnnounceInChat);
                if (ClientConfig.resolveVisibilityValue(ClientConfig.ANNOUNCE_IN_CHAT.get(), frontierAnnounceInChat)
                        && (frontier.isNamed() || ClientConfig.ANNOUNCE_UNNAMED_FRONTIERS.get())) {
                    player.displayClientMessage(Component.translatable("mapfrontiers.chat.leaving", createAnnounceText(frontier)), false);
                }
            }
        }

        for (FrontierOverlay frontier : currentlyActiveFrontiers.values()) {
            if (!announcementActiveFrontiers.containsKey(frontier.getId())
                    && (frontier.isNamed() || ClientConfig.ANNOUNCE_UNNAMED_FRONTIERS.get())) {
                Component chatAndHotbarText = createAnnounceText(frontier);
                Component titleText = createAnnounceTitle(frontier);
                Component subtitleText = createAnnounceSubtitle(frontier);

                boolean frontierAnnounceInChat = frontier.getVisibility(FrontierVisibility.AnnounceInChat);
                if (ClientConfig.resolveVisibilityValue(ClientConfig.ANNOUNCE_IN_CHAT.get(), frontierAnnounceInChat)) {
                    player.displayClientMessage(Component.translatable("mapfrontiers.chat.entering", chatAndHotbarText), false);
                }

                boolean frontierAnnounceInTitle = frontier.getVisibility(FrontierVisibility.AnnounceInTitle);
                if (ClientConfig.resolveVisibilityValue(ClientConfig.ANNOUNCE_IN_TITLE.get(), frontierAnnounceInTitle)) {
                    if (ClientConfig.TITLE_ANNOUNCEMENT_ABOVE_HOTBAR.get()) {
                        client.gui.setOverlayMessage(chatAndHotbarText, false);
                    } else if (System.currentTimeMillis() >= lastTitleTime + ClientConfig.TITLE_ANNOUNCEMENT_TIMEOUT.get() / 20 * 1000L) {
                        lastTitleTime = System.currentTimeMillis();
                        client.gui.setTimes(10, ClientConfig.TITLE_ANNOUNCEMENT_DURATION.get(), 20);
                        client.gui.setTitle(titleText);
                        client.gui.setSubtitle(subtitleText);
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

        ensureTerritoryRuntime();
        connectionState.restartHandshake();

        MapFrontiers.LOGGER.info("Client world session started");
    }

    private static void handleClientDisconnected() {
        ClientTerritoryRuntime runtime = territoryRuntime;
        territoryRuntime = null;

        if (runtime != null) {
            try {
                runtime.close();
            } catch (Throwable t) {
                MapFrontiers.LOGGER.error("Failed to close client frontier runtime", t);
            }
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

    private static Component createAnnounceTitle(FrontierOverlay frontier) {
        return createFrontierNameComponent(frontier);
    }

    private static Component createAnnounceSubtitle(FrontierOverlay frontier) {
        Component collectionComponent = createAnnouncementCollectionComponent(frontier);
        if (collectionComponent == null) {
            return Component.empty();
        }

        return Component.translatable("mapfrontiers.in_collection", collectionComponent);
    }

    private static Component createAnnounceText(FrontierOverlay frontier) {
        Component frontierName = createFrontierNameComponent(frontier);
        Component collectionComponent = createAnnouncementCollectionComponent(frontier);
        if (collectionComponent == null) {
            return frontierName;
        }

        return Component.translatable("mapfrontiers.frontier_in_collection", frontierName, collectionComponent);
    }

    private static Component createFrontierNameComponent(FrontierOverlay frontier) {
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
        text.withStyle(style -> style.withColor(
                ColorHelper.ensureMinBrightness(frontier.getColor(), ANNOUNCEMENT_MIN_BRIGHTNESS)));
        return text;
    }

    private static @Nullable Component createAnnouncementCollectionComponent(FrontierOverlay frontier) {
        if (!ClientConfig.resolveVisibilityValue(ClientConfig.MENTION_COLLECTION.get(),
                frontier.getVisibility(FrontierVisibility.MentionCollection))) {
            return null;
        }

        UUID collectionId = frontier.getCollectionId();
        if (collectionId == null) {
            return null;
        }

        CollectionData collection = getCollection(collectionId);
        if (collection == null) {
            return null;
        }

        String collectionName = collection.getName().trim();
        if (collectionName.isEmpty()) {
            return null;
        }

        MutableComponent text = Component.literal(collectionName);
        text.withStyle(style -> style.withColor(
                ColorHelper.ensureMinBrightness(collection.getColor(), ANNOUNCEMENT_MIN_BRIGHTNESS)));
        return text;
    }

    public static void setJmAPI(IClientAPI newJmAPI) {
        jmAPI = newJmAPI;
    }

    public static @Nullable IClientAPI getJmAPI() {
        return jmAPI;
    }

    public static boolean isJourneyMapPluginAvailable() {
        return jmAPI != null;
    }

    private static @Nullable ClientTerritoryRuntime ensureTerritoryRuntime() {
        if (jmAPI == null) {
            return null;
        }

        if (territoryRuntime == null) {
            territoryRuntime = new ClientTerritoryRuntime(jmAPI);
            territoryRuntime.getCollectionEvents().subscribeCreated(MapFrontiersClient.class, collection -> refreshCollectionPresentation(collection.getId()));
            territoryRuntime.getCollectionEvents().subscribeUpdated(MapFrontiersClient.class, collection -> refreshCollectionPresentation(collection.getId()));
            territoryRuntime.getCollectionEvents().subscribeDeleted(MapFrontiersClient.class, collectionId -> {
                refreshCollectionPresentation(collectionId);
                if (hud != null) {
                    hud.frontierChanged();
                }
            });
        }

        territoryRuntime.ensureInitialized();
        return territoryRuntime;
    }

    private static ClientTerritoryRuntime requireTerritoryRuntime() {
        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();
        if (runtime == null) {
            throw new IllegalStateException("JourneyMap plugin is not available.");
        }

        return runtime;
    }

    private static FrontiersOverlayManager getFrontiersOverlayManagerOrNull(boolean personal) {
        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();
        if (runtime == null) {
            return null;
        }

        if (personal) {
            return runtime.getPersonalFrontiersOverlayManager();
        }

        return runtime.getGlobalFrontiersOverlayManager();
    }

    public static void applyTerritoriesSnapshot(List<FrontierData> globalFrontiers,
                                                List<FrontierData> personalFrontiers,
                                                List<CollectionData> globalCollections,
                                                List<CollectionData> personalCollections) {
        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();
        if (runtime == null) {
            return;
        }

        if (!runtime.hasInitializedManagers()) {
            return;
        }

        MapFrontiers.LOGGER.debug("Received initial territories snapshot from server. globalFrontiers={}, personalFrontiers={}, globalCollections={}, personalCollections={}",
                globalFrontiers.size(), personalFrontiers.size(), globalCollections.size(), personalCollections.size());
        runtime.getSyncService().applyServerSnapshot(globalFrontiers, personalFrontiers, globalCollections, personalCollections);
        connectionState.markInitialTerritoriesReceived();
        publishClientApiIfReady();
        if (hud != null) {
            hud.frontierChanged();
        }
    }

    public static void applyCollectionCreated(CollectionData collection) {
        if (!isJourneyMapPluginAvailable()) {
            return;
        }

        requireTerritoryRuntime().getOperationService().applyCollectionCreated(collection);
    }

    public static void applyCollectionUpdated(CollectionData collection) {
        if (!isJourneyMapPluginAvailable()) {
            return;
        }

        requireTerritoryRuntime().getOperationService().applyCollectionUpdated(collection);
    }

    public static void applyCollectionDeleted(UUID collectionId) {
        if (!isJourneyMapPluginAvailable()) {
            return;
        }

        requireTerritoryRuntime().getOperationService().applyCollectionDeleted(collectionId);
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

    public static @Nullable CollectionData getCollection(UUID collectionId) {
        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();
        if (runtime == null) {
            return null;
        }

        return runtime.getCollectionRuntime().getCollection(collectionId);
    }

    public static List<CollectionData> getCollections(CollectionScope scope) {
        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();
        if (runtime == null) {
            return List.of();
        }

        return runtime.getCollectionRuntime().getCollections(scope);
    }

    public static List<FrontierOverlay> getFrontiersInCollection(UUID collectionId) {
        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();
        if (runtime == null) {
            return List.of();
        }

        return runtime.getCollectionRuntime().getFrontiersInCollection(collectionId);
    }

    public static List<FrontierOverlay> getFrontiersInCollection(UUID collectionId, ResourceKey<Level> dimension) {
        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();
        if (runtime == null) {
            return List.of();
        }

        return runtime.getCollectionRuntime().getFrontiersInCollection(collectionId, dimension);
    }

    public static List<FrontierOverlay> getFrontiersWithoutCollection(CollectionScope scope) {
        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();
        if (runtime == null) {
            return List.of();
        }

        return runtime.getCollectionRuntime().getFrontiersWithoutCollection(scope);
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
        ClientTerritoryRuntime runtime = requireTerritoryRuntime();
        return runtime.getLocalOverrides();
    }

    public static CollectionUiStateStore getCollectionUiStateStore() {
        ClientTerritoryRuntime runtime = requireTerritoryRuntime();
        return runtime.getCollectionUiStateStore();
    }

    public static CollectionLocalOverrides getCollectionLocalOverrides() {
        ClientTerritoryRuntime runtime = requireTerritoryRuntime();
        return runtime.getCollectionLocalOverrides();
    }

    public static ClientTerritoryOperationService getOperationService() {
        ClientTerritoryRuntime runtime = requireTerritoryRuntime();
        return runtime.getOperationService();
    }

    public static void notifyCollectionOverlayFrontierGeometryChanged(FrontierOverlay frontier) {
        if (frontier.getCollectionId() == null) {
            return;
        }

        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();
        if (runtime == null) {
            return;
        }

        runtime.getCollectionOverlayManager().markFrontierGeometryDirty(frontier, frontier.getCollectionId());
    }

    public static void setCollectionHighlighted(UUID collectionId, ResourceKey<Level> dimension, boolean highlighted) {
        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();
        if (runtime == null) {
            return;
        }

        runtime.getCollectionOverlayManager().setHighlighted(collectionId, dimension, highlighted);
    }

    public static void refreshCollectionVisibilityOverride(UUID collectionId) {
        refreshCollectionPresentation(collectionId);
    }

    public static ClientFrontierEvents getFrontierEvents() {
        ClientTerritoryRuntime runtime = requireTerritoryRuntime();
        return runtime.getFrontierEvents();
    }

    public static ClientCollectionEvents getCollectionEvents() {
        ClientTerritoryRuntime runtime = requireTerritoryRuntime();
        return runtime.getCollectionEvents();
    }

    public static ClientSettingsProfileEvents getSettingsProfileEvents() {
        ClientTerritoryRuntime runtime = requireTerritoryRuntime();
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

    public static String getKeyMappingCategory() {
        return "key.category.mapfrontiers.keybinding_category";
    }

    public static Component getOpenSettingsKey() {
        if (openSettingsKey == null || openSettingsKey.isUnbound()) {
            return null;
        } else {
            return openSettingsKey.getTranslatedKeyMessage();
        }
    }

    public static boolean matchesOpenSettingsKey(int keyCode, int scanCode) {
        return openSettingsKey != null && openSettingsKey.matches(keyCode, scanCode);
    }

    public static boolean isModOnServer() {
        return connectionState.isModOnServer();
    }

    public static void receiveSettingsProfile(SettingsProfile profile) {
        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();
        if (runtime == null) {
            return;
        }

        if (!connectionState.updateSettingsProfile(profile)) {
            return;
        }

        MapFrontiers.LOGGER.debug("Received settings profile from server.");
        runtime.getSettingsProfileEvents().postUpdated(profile);
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
        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();
        if (runtime == null) {
            return;
        }

        if (!connectionState.shouldPublishClientApi()) {
            return;
        }

        MapFrontiersAPIBootstrap.setClientAPI(runtime.getOrCreateClientApi());
        connectionState.markClientApiPublished();
        MapFrontiers.LOGGER.info(
                "Published client API. modOnServer={}, initialSettingsProfileReceived={}, initialTerritoriesSnapshotReceived={}",
                connectionState.isModOnServer(),
                connectionState.isInitialSettingsProfileReceived(),
                connectionState.isInitialTerritoriesSnapshotReceived()
        );
    }

    public static void setFrontierClipboard(FrontierData newClipboard) {
        frontierClipboard = new FrontierData(newClipboard);
    }

    public static @Nullable FrontierData getFrontierClipboard() {
        return frontierClipboard;
    }

    public static void setCollectionClipboard(CollectionData newClipboard) {
        collectionClipboard = new CollectionData(newClipboard);
    }

    public static @Nullable CollectionData getCollectionClipboard() {
        return collectionClipboard;
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
        appendHudQualifiedFrontiers(frontiers, getFrontiersOverlayManagerOrNull(true), dimension, pos, hudActiveFrontierIds,
                getMaxPathActivationDistance());
        appendHudQualifiedFrontiers(frontiers, getFrontiersOverlayManagerOrNull(false), dimension, pos, hudActiveFrontierIds,
                getMaxPathActivationDistance());
        prioritizeActiveFrontiers(frontiers);
        return frontiers;
    }

    private static Map<UUID, FrontierOverlay> collectAnnouncementActiveFrontiers(ResourceKey<Level> dimension, BlockPos pos) {
        List<FrontierOverlay> frontiers = new ArrayList<>();
        appendAnnouncementQualifiedFrontiers(frontiers, getFrontiersOverlayManagerOrNull(true), dimension, pos, announcementActiveFrontiers.keySet(),
                getMaxPathActivationDistance());
        appendAnnouncementQualifiedFrontiers(frontiers, getFrontiersOverlayManagerOrNull(false), dimension, pos, announcementActiveFrontiers.keySet(),
                getMaxPathActivationDistance());
        prioritizeActiveFrontiers(frontiers);

        Map<UUID, FrontierOverlay> activeFrontiers = new HashMap<>();
        for (FrontierOverlay frontier : frontiers) {
            activeFrontiers.put(frontier.getId(), frontier);
        }
        return activeFrontiers;
    }

    private static void appendHudQualifiedFrontiers(List<FrontierOverlay> target, @Nullable FrontiersOverlayManager manager,
                                                    ResourceKey<Level> dimension, BlockPos pos, Set<UUID> currentlyActiveFrontierIds,
                                                    double maxPathActivationDistance) {
        if (manager == null) {
            return;
        }

        int activationRadius = (int) Math.ceil(Math.max(0.0, maxPathActivationDistance));
        for (FrontierOverlay frontier : manager.getCandidateFrontiersInBounds(dimension,
                pos.getX() - activationRadius, pos.getX() + activationRadius,
                pos.getZ() - activationRadius, pos.getZ() + activationRadius)) {
            boolean alreadyActive = currentlyActiveFrontierIds.contains(frontier.getId());
            if (qualifiesForHud(frontier, pos, alreadyActive)) {
                target.add(frontier);
            }
        }
    }

    private static void appendAnnouncementQualifiedFrontiers(List<FrontierOverlay> target, @Nullable FrontiersOverlayManager manager,
                                                             ResourceKey<Level> dimension, BlockPos pos,
                                                             Set<UUID> currentlyActiveFrontierIds,
                                                             double maxPathActivationDistance) {
        if (manager == null) {
            return;
        }

        int activationRadius = (int) Math.ceil(Math.max(0.0, maxPathActivationDistance));
        for (FrontierOverlay frontier : manager.getCandidateFrontiersInBounds(dimension,
                pos.getX() - activationRadius, pos.getX() + activationRadius,
                pos.getZ() - activationRadius, pos.getZ() + activationRadius)) {
            boolean alreadyActive = currentlyActiveFrontierIds.contains(frontier.getId());
            if (qualifiesForAnnouncement(frontier, pos, alreadyActive)) {
                target.add(frontier);
            }
        }
    }

    private static boolean qualifiesForHud(FrontierOverlay frontier, BlockPos pos, boolean alreadyActive) {
        if (!ClientConfig.resolveVisibilityValue(ClientConfig.FRONTIER_VISIBILITY.get(),
                frontier.getVisibility(FrontierVisibility.Frontier))) {
            return false;
        }

        return qualifiesForActivation(frontier, pos, alreadyActive);
    }

    private static boolean qualifiesForAnnouncement(FrontierOverlay frontier, BlockPos pos, boolean alreadyActive) {
        boolean announceInChat = frontier.getVisibility(FrontierVisibility.AnnounceInChat);
        boolean announceInTitle = frontier.getVisibility(FrontierVisibility.AnnounceInTitle);
        if (!ClientConfig.resolveVisibilityValue(ClientConfig.ANNOUNCE_IN_CHAT.get(), announceInChat)
                && !ClientConfig.resolveVisibilityValue(ClientConfig.ANNOUNCE_IN_TITLE.get(), announceInTitle)) {
            return false;
        }

        return qualifiesForActivation(frontier, pos, alreadyActive);
    }

    private static boolean qualifiesForActivation(FrontierOverlay frontier, BlockPos pos, boolean alreadyActive) {

        if (frontier.getShape() == FrontierShape.Path) {
            if (frontier.getPointCount() == 0) {
                return false;
            }

            double activationDistance = ClientConfig.getPathActivationDistance(alreadyActive);
            if (!frontier.isInsideBoundingBox(pos, activationDistance)) {
                return false;
            }

            return frontier.pointIsInside(pos, activationDistance);
        }

        if (!frontier.isInsideBoundingBox(pos, 0.0)) {
            return false;
        }

        if (frontier.getShape() == FrontierShape.Vertex && frontier.getVertexCount() < 3) {
            return false;
        }

        return frontier.pointIsInside(pos, 0.0);
    }

    private static double getMaxPathActivationDistance() {
        return ClientConfig.getPathActivationDistance(true);
    }

    private static void prioritizeActiveFrontiers(List<FrontierOverlay> frontiers) {
        boolean hasAreaFrontier = frontiers.stream().anyMatch(frontier -> frontier.getShape() != FrontierShape.Path);
        if (hasAreaFrontier) {
            frontiers.removeIf(frontier -> frontier.getShape() == FrontierShape.Path);
        }

        frontiers.sort(Comparator
                .comparingDouble((FrontierOverlay frontier) -> frontier.area)
                .thenComparing(FrontierOverlay::getId));
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

    private static void refreshCollectionPresentation(UUID collectionId) {
        ClientTerritoryRuntime runtime = ensureTerritoryRuntime();

        FrontiersOverlayManager globalManager = getFrontiersOverlayManagerOrNull(false);
        if (globalManager != null) {
            globalManager.markCollectionPresentationDirty(collectionId);
        }

        FrontiersOverlayManager personalManager = getFrontiersOverlayManagerOrNull(true);
        if (personalManager != null) {
            personalManager.markCollectionPresentationDirty(collectionId);
        }

        if (runtime != null) {
            runtime.getCollectionOverlayManager().markCollectionDirty(collectionId);
        }

        if (hud != null) {
            hud.frontierChanged();
        }
    }
}
