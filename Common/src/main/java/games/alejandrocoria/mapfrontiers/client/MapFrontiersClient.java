package games.alejandrocoria.mapfrontiers.client;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.MapFrontiersAPIBootstrap;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUD;
import games.alejandrocoria.mapfrontiers.client.gui.screen.ModSettings;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.frontier.client.ClientFrontierCommandService;
import games.alejandrocoria.mapfrontiers.common.frontier.client.ClientFrontierRuntime;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandshake;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import journeymap.api.v2.client.IClientAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
    private static boolean handshakeSent = false;
    private static long handshakeNonce = 0L;
    private static long handshakeStartedAtMs = 0L;
    private static long lastHandshakeSentAtMs = 0L;
    private static boolean handshakeResolved = false;
    private static boolean modOnServer = false;
    private static boolean initialSettingsProfileReceived = false;
    private static boolean initialFrontiersReceived = false;
    private static boolean clientApiPublished = false;
    private static ClientFrontierRuntime frontierRuntime;
    private static SettingsProfile settingsProfile;
    private static ModSettings.Tab lastSettingsTab = ModSettings.Tab.Credits;

    protected static KeyMapping openSettingsKey;
    private static HUD hud;

    private static BlockPos lastPlayerPosition = new BlockPos(0, 0, 0);
    private static final Set<FrontierOverlay> insideFrontiers = new HashSet<>();
    private static long lastTitleTime;

    private static FrontierData clipboard = null;
    private static ClientLevel lastClientLevel = null;

    protected static void init() {
        ClientEventHandler.subscribeClientTickEvent(MapFrontiersClient.class, client -> {
            if (client.level == null) {
                return;
            }

            if (client.level != lastClientLevel) {
                if (!handshakeResolved) {
                    restartHandshake();
                    if (lastClientLevel != null) {
                        MapFrontiers.LOGGER.info("World changed before handshake resolution, restarting handshake.");
                    }
                }
                lastClientLevel = client.level;
            }

            processHandshake();

            ClientFrontierRuntime runtime = frontierRuntime;
            if (runtime != null && runtime.hasInitializedManagers()) {
                FrontiersOverlayManager frontiersOverlayManager = runtime.getGlobalFrontiersOverlayManager();
                FrontiersOverlayManager personalFrontiersOverlayManager = runtime.getPersonalFrontiersOverlayManager();
                if (frontiersOverlayManager != null && personalFrontiersOverlayManager != null) {
                    frontiersOverlayManager.updateAllOverlays(false);
                    personalFrontiersOverlayManager.updateAllOverlays(false);
                }
            }

            if (hud != null) {
                hud.tick();
            }
        });

        ClientEventHandler.subscribePlayerTickEvent(MapFrontiersClient.class, (client, player) -> {
            if (client.level == null) {
                return;
            }

            FrontiersOverlayManager frontiersOverlayManager = getFrontiersOverlayManagerOrNull(false);
            FrontiersOverlayManager personalFrontiersOverlayManager = getFrontiersOverlayManagerOrNull(true);
            if (frontiersOverlayManager == null || personalFrontiersOverlayManager == null) {
                return;
            }

            while (openSettingsKey != null && openSettingsKey.consumeClick()) {
                new ModSettings(false).display();
            }

            if (player == null || Config.frontierVisibility == Config.Visibility.Never) {
                return;
            }

            BlockPos currentPlayerPosition = player.blockPosition();
            if (currentPlayerPosition.getX() != lastPlayerPosition.getX() || currentPlayerPosition.getZ() != lastPlayerPosition.getZ()) {
                lastPlayerPosition = currentPlayerPosition;

                Set<FrontierOverlay> frontiers = personalFrontiersOverlayManager.getFrontiersForAnnounce(player.level().dimension(), lastPlayerPosition);
                frontiers.addAll(frontiersOverlayManager.getFrontiersForAnnounce(player.level().dimension(), lastPlayerPosition));

                for (Iterator<FrontierOverlay> i = insideFrontiers.iterator(); i.hasNext();) {
                    FrontierOverlay inside = i.next();
                    if (frontiers.stream().noneMatch(f -> f.getId().equals(inside.getId()))) {
                        boolean frontierAnnounceInChat = inside.getVisibility(FrontierData.VisibilityData.Visibility.AnnounceInChat);
                        if (Config.getVisibilityValue(Config.announceInChat, frontierAnnounceInChat) && (inside.isNamed() || Config.announceUnnamedFrontiers)) {
                            player.displayClientMessage(Component.translatable("mapfrontiers.chat.leaving", createAnnounceTextWithName(inside)), false);
                        }
                        i.remove();
                    }
                }

                for (FrontierOverlay frontier : frontiers) {
                    if (insideFrontiers.add(frontier) && (frontier.isNamed() || Config.announceUnnamedFrontiers)) {
                        Component text = createAnnounceTextWithName(frontier);

                        boolean frontierAnnounceInChat = frontier.getVisibility(FrontierData.VisibilityData.Visibility.AnnounceInChat);
                        if (Config.getVisibilityValue(Config.announceInChat, frontierAnnounceInChat)) {
                            player.displayClientMessage(Component.translatable("mapfrontiers.chat.entering", text), false);
                        }

                        boolean frontierAnnounceInTitle = frontier.getVisibility(FrontierData.VisibilityData.Visibility.AnnounceInTitle);
                        if (Config.getVisibilityValue(Config.announceInTitle, frontierAnnounceInTitle)) {
                            if (Config.titleAnnouncementAboveHotbar) {
                                client.gui.setOverlayMessage(text, false);
                            } else if (System.currentTimeMillis() >= lastTitleTime + Config.titleAnnouncementTimeout / 20 * 1000L) {
                                lastTitleTime = System.currentTimeMillis();
                                client.gui.setTimes(10, Config.titleAnnouncementDuration, 20);
                                client.gui.setTitle(text);
                            }
                        }
                    }
                }
            }
        });

        ClientEventHandler.subscribeHudRenderEvent(MapFrontiersClient.class, (graphics, delta) -> {
            if (hud == null) {
                hud = new HUD();
            } else {
                hud.drawInGameHUD(graphics, delta);
            }
        });

        ClientEventHandler.subscribeClientConnectedEvent(MapFrontiersClient.class, () -> {
            ensureFrontierRuntime();
            restartHandshake();

            MapFrontiers.LOGGER.info("ClientConnectedEvent done");
        });

        ClientEventHandler.subscribeClientDisconnectedEvent(MapFrontiersClient.class, () -> {
            if (frontierRuntime != null) {
                frontierRuntime.close();
                frontierRuntime = null;
            }

            if (hud != null) {
                hud = null;
            }

            settingsProfile = null;
            handshakeSent = false;
            handshakeResolved = false;
            modOnServer = false;
            initialSettingsProfileReceived = false;
            initialFrontiersReceived = false;
            clientApiPublished = false;
            handshakeNonce = 0L;
            handshakeStartedAtMs = 0L;
            lastHandshakeSentAtMs = 0L;
            lastClientLevel = null;
            MapFrontiersAPIBootstrap.clearClientAPI();

            ChatFrontiers.clear();

            MapFrontiers.LOGGER.info("ClientDisconnectedEvent done");
        });
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

    public static void setjmAPI(IClientAPI newJmAPI) {
        jmAPI = newJmAPI;
        if (frontierRuntime != null) {
            frontierRuntime.setJourneyMapApi(newJmAPI);
        }
    }

    private static ClientFrontierRuntime ensureFrontierRuntime() {
        if (frontierRuntime == null) {
            frontierRuntime = new ClientFrontierRuntime(jmAPI);
            frontierRuntime.getSettingsProfileBridge().subscribeUpdated(MapFrontiersClient.class, profile -> {
                settingsProfile = profile;
                initialSettingsProfileReceived = true;
                MapFrontiers.LOGGER.debug("Received settings profile from server.");
                resolveHandshake(true, HandshakeSignal.SETTINGS_PROFILE);
                tryPublishClientApi();
            });
        } else {
            frontierRuntime.setJourneyMapApi(jmAPI);
        }

        frontierRuntime.ensureInitialized();
        return frontierRuntime;
    }

    private static FrontiersOverlayManager getFrontiersOverlayManagerOrNull(boolean personal) {
        ClientFrontierRuntime runtime = ensureFrontierRuntime();
        if (personal) {
            return runtime.getPersonalFrontiersOverlayManager();
        }

        return runtime.getGlobalFrontiersOverlayManager();
    }

    public static void setFrontiersFromServer(List<FrontierData> globalFrontiers, List<FrontierData> personalFrontiers) {
        ClientFrontierRuntime runtime = ensureFrontierRuntime();
        if (!runtime.hasInitializedManagers()) {
            return;
        }

        MapFrontiers.LOGGER.debug("Received initial frontier snapshot from server. global={}, personal={}",
                globalFrontiers.size(), personalFrontiers.size());
        runtime.getSyncService().applyServerSnapshot(globalFrontiers, personalFrontiers);
        initialFrontiersReceived = true;
        tryPublishClientApi();
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

    public static List<FrontierOverlay> getFrontiersInPosition(ResourceKey<Level> dimension, BlockPos pos) {
        return getFrontiersInPosition(dimension, pos, 0.0);
    }

    public static List<FrontierOverlay> getFrontiersInPosition(ResourceKey<Level> dimension, BlockPos pos, double maxDistanceToOpen) {
        FrontiersOverlayManager personalFrontiersOverlayManager = getFrontiersOverlayManagerOrNull(true);
        FrontiersOverlayManager frontiersOverlayManager = getFrontiersOverlayManagerOrNull(false);
        if (personalFrontiersOverlayManager == null || frontiersOverlayManager == null) {
            return List.of();
        }

        List<FrontierOverlay> frontiers = personalFrontiersOverlayManager.getFrontiersInPosition(dimension, pos, maxDistanceToOpen);
        frontiers.addAll(frontiersOverlayManager.getFrontiersInPosition(dimension, pos, maxDistanceToOpen));
        frontiers.sort((f1, f2) -> Float.compare(f1.area, f2.area));
        return frontiers;
    }

    public static FrontierLocalOverrides getLocalOverrides() {
        ClientFrontierRuntime runtime = ensureFrontierRuntime();
        return runtime.getLocalOverrides();
    }

    public static ClientFrontierCommandService getCommandService() {
        ClientFrontierRuntime runtime = ensureFrontierRuntime();
        return runtime.getCommandService();
    }

    public static SettingsProfile getSettingsProfile() {
        return settingsProfile;
    }

    public static void setLastSettingsTab(ModSettings.Tab tab) {
        lastSettingsTab = tab;
    }

    public static ModSettings.Tab getLastSettingsTab() {
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
        return modOnServer;
    }

    public static void receiveSettingsProfile(SettingsProfile profile) {
        SettingsProfile currentProfile = settingsProfile;
        if (currentProfile != null && currentProfile.equals(profile)) {
            return;
        }

        ensureFrontierRuntime().getSettingsProfileBridge().postUpdated(profile);
    }

    public static void receiveHandshakeAck(long nonce) {
        if (!handshakeResolved && nonce == handshakeNonce) {
            MapFrontiers.LOGGER.debug("Received handshake acknowledgment from server.");
            resolveHandshake(true, HandshakeSignal.ACK);
            return;
        }

        if (handshakeResolved && !modOnServer && nonce == handshakeNonce) {
            MapFrontiers.LOGGER.debug("Received late handshake acknowledgment from server.");
            upgradeToModOnServer(HandshakeSignal.ACK);
        }
    }

    private static void processHandshake() {
        if (handshakeResolved) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!handshakeSent || now - lastHandshakeSentAtMs >= HANDSHAKE_RETRY_MS) {
            if (!handshakeSent) {
                handshakeNonce = now;
                handshakeStartedAtMs = now;
                MapFrontiers.LOGGER.debug("Sending initial handshake to server.");
            }

            handshakeSent = true;
            lastHandshakeSentAtMs = now;
            PacketHandler.sendToServer(new PacketHandshake(handshakeNonce));
        }

        if (handshakeStartedAtMs > 0L && now - handshakeStartedAtMs >= HANDSHAKE_TIMEOUT_MS) {
            MapFrontiers.LOGGER.debug("Handshake timed out after {} ms.", HANDSHAKE_TIMEOUT_MS);
            resolveHandshake(false, HandshakeSignal.TIMEOUT);
        }
    }

    private static void restartHandshake() {
        handshakeSent = false;
        handshakeResolved = false;
        modOnServer = false;
        handshakeNonce = 0L;
        handshakeStartedAtMs = 0L;
        lastHandshakeSentAtMs = 0L;
        initialSettingsProfileReceived = false;
        initialFrontiersReceived = false;
        clientApiPublished = false;
        settingsProfile = null;
    }

    private static void resolveHandshake(boolean hasModOnServer, HandshakeSignal source) {
        if (handshakeResolved) {
            if (!modOnServer && hasModOnServer) {
                upgradeToModOnServer(source);
            }
            return;
        }

        handshakeResolved = true;
        modOnServer = hasModOnServer;
        tryPublishClientApi();
        MapFrontiers.LOGGER.info("Handshake resolved from {}. mapfrontiers on server: {}",
                source.displayName(), modOnServer);
    }

    private static void upgradeToModOnServer(HandshakeSignal source) {
        modOnServer = true;

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

    private static void ensureClientApiInitialized() {
        ensureFrontierRuntime();
    }

    private static void tryPublishClientApi() {
        if (!handshakeResolved || clientApiPublished) {
            return;
        }

        if (modOnServer && (!initialSettingsProfileReceived || !initialFrontiersReceived)) {
            return;
        }

        ensureClientApiInitialized();
        MapFrontiersAPIBootstrap.setClientAPI(frontierRuntime.getOrCreateClientApi());
        clientApiPublished = true;
        MapFrontiers.LOGGER.info(
                "Published client API. modOnServer={}, initialSettingsProfileReceived={}, initialFrontiersReceived={}",
                modOnServer, initialSettingsProfileReceived, initialFrontiersReceived
        );
    }

    public static void setClipboard(FrontierData newClipboard) {
        clipboard = new FrontierData(newClipboard);
    }

    public static FrontierData getClipboard() {
        return clipboard;
    }
}
