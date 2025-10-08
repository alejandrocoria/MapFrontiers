package games.alejandrocoria.mapfrontiers.client;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUD;
import games.alejandrocoria.mapfrontiers.client.gui.screen.ModSettings;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
public class MapFrontiersClient {
    private static IClientAPI jmAPI;
    private static boolean handshakeSent = false;
    private static FrontiersOverlayManager frontiersOverlayManager;
    private static FrontiersOverlayManager personalFrontiersOverlayManager;
    private static FrontierLocalOverrides localOverrides;
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
        ClientEventHandler.subscribeUpdatedSettingsProfileEvent(MapFrontiersClient.class, profile -> settingsProfile = profile);

        ClientEventHandler.subscribeClientTickEvent(MapFrontiersClient.class, client -> {
            if (client.level == null) {
                return;
            }

            if (client.level != lastClientLevel) {
                if (settingsProfile == null) {
                    handshakeSent = false;
                    MapFrontiers.LOGGER.info("World changed and not synchronized with server, attempting handshake again.");
                }
                lastClientLevel = client.level;
            }

            if (!handshakeSent) {
                handshakeSent = true;
                PacketHandler.sendToServer(new PacketHandshake());
            }

            if (frontiersOverlayManager != null) {
                frontiersOverlayManager.updateAllOverlays(false);
                personalFrontiersOverlayManager.updateAllOverlays(false);
            }

            if (hud != null) {
                hud.tick();
            }
        });

        ClientEventHandler.subscribePlayerTickEvent(MapFrontiersClient.class, (client, player) -> {
            if (client.level == null) {
                return;
            }

            if (frontiersOverlayManager == null) {
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
            initializeManagers();

            MapFrontiers.LOGGER.info("ClientConnectedEvent done");
        });

        ClientEventHandler.subscribeClientDisconnectedEvent(MapFrontiersClient.class, () -> {
            if (frontiersOverlayManager != null) {
                frontiersOverlayManager.close();
                frontiersOverlayManager = null;
                personalFrontiersOverlayManager.close();
                personalFrontiersOverlayManager = null;
                localOverrides = null;
            }

            if (hud != null) {
                hud = null;
            }

            settingsProfile = null;
            handshakeSent = false;
            lastClientLevel = null;

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
    }

    private static void initializeManagers() {
        if (jmAPI == null) {
            return;
        }

        if (frontiersOverlayManager == null) {
            frontiersOverlayManager = new FrontiersOverlayManager(jmAPI, false);
        }

        if (personalFrontiersOverlayManager == null) {
            personalFrontiersOverlayManager = new FrontiersOverlayManager(jmAPI, true);
        }

        if (localOverrides == null) {
            localOverrides = new FrontierLocalOverrides();
        }
    }

    public static void setFrontiersFromServer(List<FrontierData> globalFrontiers, List<FrontierData> personalFrontiers) {
        initializeManagers();
        frontiersOverlayManager.setFrontiersFromServer(globalFrontiers);
        personalFrontiersOverlayManager.setFrontiersFromServer(personalFrontiers);
        if (hud != null) {
            hud.frontierChanged();
        }
    }

    public static FrontiersOverlayManager getFrontiersOverlayManager(boolean personal) {
        initializeManagers();

        if (personal) {
            return personalFrontiersOverlayManager;
        } else {
            return frontiersOverlayManager;
        }
    }

    public static List<FrontierOverlay> getFrontiersInPosition(ResourceKey<Level> dimension, BlockPos pos) {
        return getFrontiersInPosition(dimension, pos, 0.0);
    }

    public static List<FrontierOverlay> getFrontiersInPosition(ResourceKey<Level> dimension, BlockPos pos, double maxDistanceToOpen) {
        initializeManagers();

        List<FrontierOverlay> frontiers = personalFrontiersOverlayManager.getFrontiersInPosition(dimension, pos, maxDistanceToOpen);
        frontiers.addAll(frontiersOverlayManager.getFrontiersInPosition(dimension, pos, maxDistanceToOpen));
        frontiers.sort((f1, f2) -> Float.compare(f1.area, f2.area));
        return frontiers;
    }

    public static FrontierLocalOverrides getLocalOverrides() {
        return localOverrides;
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

    public static Component getOpenSettingsKey() {
        if (openSettingsKey == null || openSettingsKey.isUnbound()) {
            return null;
        } else {
            return openSettingsKey.getTranslatedKeyMessage();
        }
    }

    public static boolean isModOnServer() {
        return settingsProfile != null;
    }

    public static void setClipboard(FrontierData newClipboard) {
        clipboard = new FrontierData(newClipboard);
    }

    public static FrontierData getClipboard() {
        return clipboard;
    }
}