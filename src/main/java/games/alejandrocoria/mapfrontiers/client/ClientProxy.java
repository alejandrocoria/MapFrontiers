package games.alejandrocoria.mapfrontiers.client;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.eventbus.api.EventPriority;
import org.lwjgl.glfw.GLFW;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;
import games.alejandrocoria.mapfrontiers.client.gui.GuiHUD;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import journeymap.client.api.IClientAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedInEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MapFrontiers.MODID)
@OnlyIn(Dist.CLIENT)
public class ClientProxy {
    private static IClientAPI jmAPI;
    private static FrontiersOverlayManager frontiersOverlayManager;
    private static FrontiersOverlayManager personalFrontiersOverlayManager;
    private static SettingsProfile settingsProfile;
    private static GuiFrontierSettings.Tab lastSettingsTab = GuiFrontierSettings.Tab.Credits;

    private static KeyMapping openSettingsKey;
    private static GuiHUD guiHUD;

    private static ItemStack bookItemInHand;

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        Minecraft.getInstance().getMainRenderTarget().enableStencil();
        MinecraftForge.EVENT_BUS.register(FrontierOverlay.class);
        MinecraftForge.EVENT_BUS.register(FrontiersOverlayManager.class);

        openSettingsKey = new KeyMapping("mapfrontiers.key.open_settings", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "mapfrontiers.key.category");
        ClientRegistry.registerKeyBinding(openSettingsKey);

        MapFrontiers.LOGGER.info("clientSetup done");
    }

    @SubscribeEvent
    public static void onEvent(KeyInputEvent event) {
        if (openSettingsKey.matches(event.getKey(), event.getScanCode()) && openSettingsKey.isDown()) {
            if (frontiersOverlayManager == null) {
                return;
            }

            if (settingsProfile == null) {
                return;
            }

            Minecraft.getInstance().setScreen(new GuiFrontierSettings());
        }
    }

    public static BlockPos snapVertex(BlockPos vertex, float snapDistance, ResourceKey<Level> dimension,
            @Nullable FrontierData owner) {
        BlockPos closest = vertex.atY(70);
        double closestDistance = snapDistance * snapDistance;

        for (FrontierData frontier : personalFrontiersOverlayManager.getAllFrontiers(dimension)) {
            if (frontier == owner) {
                continue;
            }

            for (int i = 0; i < frontier.getVertexCount(); ++i) {
                BlockPos v = frontier.getVertex(i).atY(70);
                double distance = v.distSqr(closest, false);
                if (distance <= closestDistance) {
                    closestDistance = distance;
                    closest = v;
                }
            }
        }

        for (FrontierData frontier : frontiersOverlayManager.getAllFrontiers(dimension)) {
            if (frontier == owner) {
                continue;
            }

            for (int i = 0; i < frontier.getVertexCount(); ++i) {
                BlockPos v = frontier.getVertex(i).atY(70);
                double distance = v.distSqr(closest, false);
                if (distance <= closestDistance) {
                    closestDistance = distance;
                    closest = v;
                }
            }
        }

        return closest;
    }

    public static void setjmAPI(IClientAPI newJmAPI) {
        jmAPI = newJmAPI;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            ItemStack itemMainhand = player.getItemBySlot(EquipmentSlot.MAINHAND);
            ItemStack itemOffhand = player.getItemBySlot(EquipmentSlot.OFFHAND);

            if (itemMainhand != bookItemInHand && itemOffhand != bookItemInHand) {
                bookItemInHand = null;

                if (!itemMainhand.isEmpty() && (itemMainhand.getItem() == MapFrontiers.frontierBook
                        || itemMainhand.getItem() == MapFrontiers.personalFrontierBook)) {
                    if (itemMainhand.hasTag()) {
                        CompoundTag nbt = itemMainhand.getTag();
                        if (nbt != null && nbt.contains("Dimension")
                                && nbt.getString("Dimension").equals(player.level.dimension().location().toString())) {
                            bookItemInHand = itemMainhand;
                        }
                    }
                }

                if (bookItemInHand == null && !itemOffhand.isEmpty() && (itemOffhand.getItem() == MapFrontiers.frontierBook
                        || itemMainhand.getItem() == MapFrontiers.personalFrontierBook)) {
                    if (itemOffhand.hasTag()) {
                        CompoundTag nbt = itemOffhand.getTag();
                        if (nbt != null && nbt.contains("Dimension")
                                && nbt.getString("Dimension").equals(player.level.dimension().location().toString())) {
                            bookItemInHand = itemOffhand;
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void clientConnectedToServer(LoggedInEvent event) {
        if (jmAPI != null) {
            bookItemInHand = null;
            if (frontiersOverlayManager != null) {
                frontiersOverlayManager.removeAllOverlays();
                personalFrontiersOverlayManager.removeAllOverlays();
            }
            frontiersOverlayManager = new FrontiersOverlayManager(jmAPI, false);
            personalFrontiersOverlayManager = new FrontiersOverlayManager(jmAPI, true);

            guiHUD = new GuiHUD(frontiersOverlayManager, personalFrontiersOverlayManager);
            MinecraftForge.EVENT_BUS.register(guiHUD);
        }
    }

    @SubscribeEvent
    public static void clientDisconnectionFromServer(LoggedOutEvent event) {
        bookItemInHand = null;

        if (frontiersOverlayManager != null) {
            frontiersOverlayManager.removeAllOverlays();
            frontiersOverlayManager = null;
            personalFrontiersOverlayManager.removeAllOverlays();
            personalFrontiersOverlayManager = null;
        }

        if (guiHUD != null) {
            MinecraftForge.EVENT_BUS.unregister(guiHUD);
            guiHUD = null;
        }
    }

    public static void openGUIFrontierBook() {
        Minecraft.getInstance().setScreen(new GuiFrontierBook());
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

    public static boolean hasBookItemInHand() {
        return bookItemInHand != null;
    }

    public static FrontiersOverlayManager getFrontiersOverlayManager(boolean personal) {
        if (personal) {
            return personalFrontiersOverlayManager;
        } else {
            return frontiersOverlayManager;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUpdatedSettingsProfileEvent(UpdatedSettingsProfileEvent event) {
        settingsProfile = event.profile;
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
        ConfigData.save();

        if (frontiersOverlayManager != null) {
            frontiersOverlayManager.updateAllOverlays();
            personalFrontiersOverlayManager.updateAllOverlays();
        }

        if (guiHUD != null) {
            guiHUD.configUpdated(Minecraft.getInstance().getWindow());
        }
    }
}
