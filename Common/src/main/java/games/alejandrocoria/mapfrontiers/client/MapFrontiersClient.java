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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
public class MapFrontiersClient {
    private static IClientAPI jmAPI;
    private static boolean handshakeSended = false;
    private static FrontiersOverlayManager frontiersOverlayManager;
    private static FrontiersOverlayManager personalFrontiersOverlayManager;
    private static SettingsProfile settingsProfile;
    private static ModSettings.Tab lastSettingsTab = ModSettings.Tab.Credits;

    protected static KeyMapping openSettingsKey;
    private static HUD hud;

    private static BlockPos lastPlayerPosition = new BlockPos(0, 0, 0);
    private static final Set<FrontierOverlay> insideFrontiers = new HashSet<>();

    private static FrontierData clipboard = null;

    protected static void init() {
        ClientEventHandler.subscribeUpdatedSettingsProfileEvent(MapFrontiersClient.class, profile -> settingsProfile = profile);

        ClientEventHandler.subscribeClientTickEvent(MapFrontiersClient.class, client -> {
            if (client.level == null) {
                return;
            }

            if (!handshakeSended) {
                handshakeSended = true;
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
                client.setScreen(new ModSettings(false, null));
            }

            if (player == null) {
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
                        if (inside.getAnnounceInChat() && (inside.isNamed() || Config.announceUnnamedFrontiers)) {
                            player.displayClientMessage(Component.translatable("mapfrontiers.chat.leaving", createAnnounceTextWithName(inside)), false);
                        }
                        i.remove();
                    }
                }

                for (FrontierOverlay frontier : frontiers) {
                    if (insideFrontiers.add(frontier) && (frontier.isNamed() || Config.announceUnnamedFrontiers)) {
                        Component text = createAnnounceTextWithName(frontier);
                        if (frontier.getAnnounceInChat()) {
                            player.displayClientMessage(Component.translatable("mapfrontiers.chat.entering", text), false);
                        }
                        if (frontier.getAnnounceInTitle()) {
                            if (Config.titleAnnouncementAboveHotbar) {
                                client.gui.setOverlayMessage(text, false);
                            } else {
                                client.gui.setTitle(text);
                            }
                        }
                    }
                }
            }
        });

        ClientEventHandler.subscribeHudRenderEvent(MapFrontiersClient.class, (graphics, delta) -> {
            if (hud != null) {
                hud.drawInGameHUD(graphics, delta);
            }
        });

        ClientEventHandler.subscribeClientConnectedEvent(MapFrontiersClient.class, () -> {
            initializeManagers();
            hud = new HUD(frontiersOverlayManager, personalFrontiersOverlayManager);

            MapFrontiers.LOGGER.info("ClientConnectedEvent done");
        });

        ClientEventHandler.subscribeClientDisconnectedEvent(MapFrontiersClient.class, () -> {
            if (frontiersOverlayManager != null) {
                frontiersOverlayManager.close();
                frontiersOverlayManager = null;
                personalFrontiersOverlayManager.close();
                personalFrontiersOverlayManager = null;
            }

            if (hud != null) {
                hud = null;
            }

            settingsProfile = null;
            handshakeSended = false;

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
    }

    public static void setFrontiersFromServer(List<FrontierData> globalFrontiers, List<FrontierData> personalFrontiers) {
        initializeManagers();
        frontiersOverlayManager.setFrontiersFromServer(globalFrontiers);
        personalFrontiersOverlayManager.setFrontiersFromServer(personalFrontiers);
    }

    public static FrontiersOverlayManager getFrontiersOverlayManager(boolean personal) {
        initializeManagers();

        if (personal) {
            return personalFrontiersOverlayManager;
        } else {
            return frontiersOverlayManager;
        }
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
