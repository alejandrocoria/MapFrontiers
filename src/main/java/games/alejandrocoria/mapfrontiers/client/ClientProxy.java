package games.alejandrocoria.mapfrontiers.client;

import com.mojang.blaze3d.platform.InputConstants;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;
import games.alejandrocoria.mapfrontiers.client.gui.GuiHUD;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.util.BlockPosHelper;
import journeymap.client.api.IClientAPI;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
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

    @Override
    public void onInitializeClient() {
        AutoConfig.register(ConfigData.class, Toml4jConfigSerializer::new);
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
            while (openSettingsKey.consumeClick()) {
                if (frontiersOverlayManager == null) {
                    return;
                }

                if (settingsProfile == null) {
                    return;
                }

                Minecraft.getInstance().setScreen(new GuiFrontierSettings());
            }
        });

        HudRenderCallback.EVENT.register((matrixStack, delta) -> {
            if (guiHUD != null) {
                guiHUD.drawInGameHUD(matrixStack, delta);
            }
        });

        ClientLoginConnectionEvents.INIT.register((handler, client) -> {
            if (jmAPI != null) {
                if (frontiersOverlayManager != null) {
                    frontiersOverlayManager.removeAllOverlays();
                    personalFrontiersOverlayManager.removeAllOverlays();
                }
                frontiersOverlayManager = new FrontiersOverlayManager(jmAPI, false);
                personalFrontiersOverlayManager = new FrontiersOverlayManager(jmAPI, true);

                guiHUD = new GuiHUD(frontiersOverlayManager, personalFrontiersOverlayManager);
            }
        });

        ClientLoginConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (frontiersOverlayManager != null) {
                frontiersOverlayManager.removeAllOverlays();
                frontiersOverlayManager = null;
                personalFrontiersOverlayManager.removeAllOverlays();
                personalFrontiersOverlayManager = null;
            }

            if (guiHUD != null) {
                guiHUD = null;
            }
        });

        MapFrontiers.LOGGER.info("onInitializeClient done");
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

    public static void configUpdated() {
        AutoConfig.getConfigHolder(ConfigData.class).save();

        if (frontiersOverlayManager != null) {
            frontiersOverlayManager.updateAllOverlays(true);
            personalFrontiersOverlayManager.updateAllOverlays(true);
        }

        if (guiHUD != null) {
            guiHUD.configUpdated();
        }
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
