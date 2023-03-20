package games.alejandrocoria.mapfrontiers.client;

import com.mojang.blaze3d.platform.InputConstants;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiColors;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;
import games.alejandrocoria.mapfrontiers.client.gui.GuiHUD;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.util.BlockPosHelper;
import journeymap.client.api.IClientAPI;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class ClientProxy implements ClientModInitializer {
    private static IClientAPI jmAPI;
    private static FrontiersOverlayManager frontiersOverlayManager;
    private static FrontiersOverlayManager personalFrontiersOverlayManager;
    private static SettingsProfile settingsProfile;
    private static GuiFrontierSettings.Tab lastSettingsTab = GuiFrontierSettings.Tab.Credits;

    private static KeyMapping openSettingsKey;
    private static GuiHUD guiHUD;

    private static final Map<Object, Consumer<UUID>> deletedFrontierEventMap = new HashMap<>();
    private static final Map<Object, BiConsumer<FrontierOverlay, Integer>> newFrontierEventMap = new HashMap<>();
    private static final Map<Object, BiConsumer<FrontierOverlay, Integer>> updatedFrontierEventMap = new HashMap<>();
    private static final Map<Object, Consumer<SettingsProfile>> updatedSettingsProfileEventMap = new HashMap<>();

    private static BlockPos lastPlayerPosition = new BlockPos(0, 0, 0);
    private static Set<FrontierOverlay> insideFrontiers = new HashSet<>();

    @Override
    public void onInitializeClient() {
        ModLoadingContext.registerConfig(MapFrontiers.MODID, ModConfig.Type.CLIENT, ConfigData.CLIENT_SPEC);
        ConfigData.bakeConfig();
        PacketHandler.registerClientReceivers();

        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "mapfrontiers.key.open_settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "mapfrontiers.key.category"
        ));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (frontiersOverlayManager != null) {
                frontiersOverlayManager.updateAllOverlays(false);
                personalFrontiersOverlayManager.updateAllOverlays(false);
            }

            if (guiHUD != null) {
                guiHUD.tick();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (frontiersOverlayManager == null) {
                return;
            }

            while (openSettingsKey.consumeClick()) {
                Minecraft.getInstance().setScreen(new GuiFrontierSettings(null, false));
            }

            Player player = client.player;

            if (player == null) {
                return;
            }

            BlockPos currentPlayerPosition = player.blockPosition();
            if (currentPlayerPosition.getX() != lastPlayerPosition.getX() || currentPlayerPosition.getZ() != lastPlayerPosition.getZ()) {
                lastPlayerPosition = currentPlayerPosition;

                Set<FrontierOverlay> frontiers = personalFrontiersOverlayManager.getFrontiersForAnnounce(player.level.dimension(), lastPlayerPosition);
                frontiers.addAll(frontiersOverlayManager.getFrontiersForAnnounce(player.level.dimension(), lastPlayerPosition));

                for (Iterator<FrontierOverlay> i = insideFrontiers.iterator(); i.hasNext();) {
                    FrontierOverlay inside = i.next();
                    if (frontiers.stream().noneMatch(f -> f.getId().equals(inside.getId()))) {
                        if (inside.getAnnounceInChat() && (inside.isNamed() || ConfigData.announceUnnamedFrontiers)) {
                            player.displayClientMessage(new TranslatableComponent("mapfrontiers.chat.leaving", createAnnounceTextWithName(inside)), false);
                        }
                        i.remove();
                    }
                }

                for (FrontierOverlay frontier : frontiers) {
                    if (insideFrontiers.add(frontier) && (frontier.isNamed() || ConfigData.announceUnnamedFrontiers)) {
                        Component text = createAnnounceTextWithName(frontier);
                        if (frontier.getAnnounceInChat()) {
                            player.displayClientMessage(new TranslatableComponent("mapfrontiers.chat.entering", text), false);
                        }
                        if (frontier.getAnnounceInTitle()) {
                            if (ConfigData.titleAnnouncementAboveHotbar) {
                                Minecraft.getInstance().gui.setOverlayMessage(text, false);
                            } else {
                                Minecraft.getInstance().gui.setTitle(text);
                            }
                        }
                    }
                }
            }
        });

        HudRenderCallback.EVENT.register((matrixStack, delta) -> {
            if (guiHUD != null) {
                guiHUD.drawInGameHUD(matrixStack, delta);
            }
        });

        ClientLoginConnectionEvents.INIT.register((handler, client) -> {
            initializeManagers();
            guiHUD = new GuiHUD(frontiersOverlayManager, personalFrontiersOverlayManager);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (frontiersOverlayManager != null) {
                frontiersOverlayManager.removeAllOverlays();
                frontiersOverlayManager = null;
                personalFrontiersOverlayManager.removeAllOverlays();
                personalFrontiersOverlayManager = null;
            }

            if (guiHUD != null) {
                guiHUD = null;
            }

            settingsProfile = null;
        });

        MapFrontiers.LOGGER.info("onInitializeClient done");
    }

    private static Component createAnnounceTextWithName(FrontierOverlay frontier) {
        if (!frontier.isNamed()) {
            MutableComponent text = new TranslatableComponent("mapfrontiers.unnamed", ChatFormatting.ITALIC);
            text.withStyle(style -> style.withItalic(true).withColor(GuiColors.SETTINGS_TEXT_MEDIUM));
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

        MutableComponent text = new TextComponent(name);
        text.withStyle(style -> style.withColor(frontier.getColor()));
        return text;
    }

    public static BlockPos snapVertex(BlockPos vertex, float snapDistance, ResourceKey<Level> dimension, @Nullable FrontierOverlay owner) {
        BlockPos closest = BlockPosHelper.atY(vertex,70);
        double closestDistance = snapDistance * snapDistance;

        for (FrontierOverlay frontier : personalFrontiersOverlayManager.getAllFrontiers(dimension)) {
            if (frontier == owner) {
                continue;
            }

            BlockPos v = frontier.getClosestVertex(closest, closestDistance);
            if (v != null) {
                closest = v;
                closestDistance = v.distSqr(vertex);
            }
        }

        for (FrontierOverlay frontier : frontiersOverlayManager.getAllFrontiers(dimension)) {
            if (frontier == owner) {
                continue;
            }

            BlockPos v = frontier.getClosestVertex(closest, closestDistance);
            if (v != null) {
                closest = v;
                closestDistance = v.distSqr(vertex);
            }
        }

        return closest;
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

    public static ItemStack getHeldBanner() {
        ItemStack mainhand = Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack offhand = Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.OFFHAND);
        ItemStack heldBanner = null;

        if (mainhand.getItem() instanceof BannerItem) {
            heldBanner = mainhand;
        } else if (offhand.getItem() instanceof BannerItem) {
            heldBanner = offhand;
        }

        return heldBanner;
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

    public static void setLastSettingsTab(GuiFrontierSettings.Tab tab) {
        lastSettingsTab = tab;
    }

    public static GuiFrontierSettings.Tab getLastSettingsTab() {
        return lastSettingsTab;
    }

    public static Component getOpenSettingsKey() {
        if (openSettingsKey.isUnbound()) {
            return null;
        } else {
            return openSettingsKey.getTranslatedKeyMessage();
        }
    }

    public static void configUpdated() {
        ConfigData.save();

        if (frontiersOverlayManager != null) {
            frontiersOverlayManager.updateAllOverlays(true);
            personalFrontiersOverlayManager.updateAllOverlays(true);
        }

        if (guiHUD != null) {
            guiHUD.configUpdated();
        }
    }

    public static boolean isModOnServer() {
        return settingsProfile != null;
    }

    public static void subscribeDeletedFrontierEvent(Object object, Consumer<UUID> callback) {
        deletedFrontierEventMap.put(object, callback);
    }

    public static void subscribeNewFrontierEvent(Object object, BiConsumer<FrontierOverlay, Integer> callback) {
        newFrontierEventMap.put(object, callback);
    }

    public static void subscribeUpdatedFrontierEvent(Object object, BiConsumer<FrontierOverlay, Integer> callback) {
        updatedFrontierEventMap.put(object, callback);
    }

    public static void subscribeUpdatedSettingsProfileEvent(Object object, Consumer<SettingsProfile> callback) {
        updatedSettingsProfileEventMap.put(object, callback);
    }

    public static void unsuscribeAllEvents(Object object) {
        deletedFrontierEventMap.remove(object);
        newFrontierEventMap.remove(object);
        updatedFrontierEventMap.remove(object);
        updatedSettingsProfileEventMap.remove(object);
    }

    public static void postDeletedFrontierEvent(UUID frontierID) {
        for (Consumer<UUID> callback : deletedFrontierEventMap.values()) {
            callback.accept(frontierID);
        }
    }

    public static void postNewFrontierEvent(FrontierOverlay frontierOverlay, int playerID) {
        for (BiConsumer<FrontierOverlay, Integer> callback : newFrontierEventMap.values()) {
            callback.accept(frontierOverlay, playerID);
        }
    }

    public static void postUpdatedFrontierEvent(FrontierOverlay frontierOverlay, int playerID) {
        for (BiConsumer<FrontierOverlay, Integer> callback : updatedFrontierEventMap.values()) {
            callback.accept(frontierOverlay, playerID);
        }
    }

    public static void postUpdatedSettingsProfileEvent(SettingsProfile profile) {
        settingsProfile = profile;

        for (Consumer<SettingsProfile> callback : updatedSettingsProfileEventMap.values()) {
            callback.accept(profile);
        }
    }
}
