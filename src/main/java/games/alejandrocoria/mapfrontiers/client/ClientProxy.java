package games.alejandrocoria.mapfrontiers.client;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.inventory.EquipmentSlotType;
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
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedInEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MapFrontiers.MODID)
public class ClientProxy {
    private static IClientAPI jmAPI;
    private static FrontiersOverlayManager frontiersOverlayManager;
    private static FrontiersOverlayManager personalFrontiersOverlayManager;
    private static SettingsProfile settingsProfile;
    private static GuiFrontierSettings.Tab lastSettingsTab = GuiFrontierSettings.Tab.Credits;

    private static KeyBinding openSettingsKey;
    private static GuiHUD guiHUD;

    private static ItemStack bookItemInHand;

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        Minecraft.getInstance().getMainRenderTarget().enableStencil();
        MinecraftForge.EVENT_BUS.register(Sounds.class);
        MinecraftForge.EVENT_BUS.register(FrontierOverlay.class);
        MinecraftForge.EVENT_BUS.register(FrontiersOverlayManager.class);

        openSettingsKey = new KeyBinding("mapfrontiers.key.open_settings", KeyConflictContext.IN_GAME,
                InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_F8, "mapfrontiers.key.category");
        ClientRegistry.registerKeyBinding(openSettingsKey);

        MapFrontiers.LOGGER.info("clientSetup done");
    }

    @SubscribeEvent
    public static void onEvent(KeyInputEvent event) {
        if (openSettingsKey.isDown()) {
            if (frontiersOverlayManager == null) {
                return;
            }

            if (settingsProfile == null) {
                return;
            }

            Minecraft.getInstance().setScreen(new GuiFrontierSettings(settingsProfile));
        }
    }

    public static BlockPos snapVertex(BlockPos vertex, int snapDistance, RegistryKey<World> dimension,
            @Nullable FrontierData owner) {
        float snapDistanceSq = snapDistance * snapDistance;
        BlockPos closest = new BlockPos(vertex.getX(), 70, vertex.getZ());
        double closestDistance = Double.MAX_VALUE;

        for (FrontierData frontier : personalFrontiersOverlayManager.getAllFrontiers(dimension)) {
            if (frontier == owner) {
                continue;
            }

            for (int i = 0; i < frontier.getVertexCount(); ++i) {
                BlockPos v = frontier.getVertex(i);
                BlockPos v2 = new BlockPos(v.getX(), 70, v.getZ());
                double distance = v2.distSqr(closest);
                if (distance < snapDistanceSq && distance < closestDistance && !containsVertex(owner, v2)) {
                    closestDistance = distance;
                    closest = v2;
                }
            }
        }

        for (FrontierData frontier : frontiersOverlayManager.getAllFrontiers(dimension)) {
            if (frontier == owner) {
                continue;
            }

            for (int i = 0; i < frontier.getVertexCount(); ++i) {
                BlockPos v = frontier.getVertex(i);
                BlockPos v2 = new BlockPos(v.getX(), 70, v.getZ());
                double distance = v2.distSqr(closest);
                if (distance < snapDistanceSq && distance < closestDistance && !containsVertex(owner, v2)) {
                    closestDistance = distance;
                    closest = v2;
                }
            }
        }

        return closest;
    }

    private static boolean containsVertex(@Nullable FrontierData frontier, BlockPos vertex) {
        if (frontier == null) {
            return false;
        }

        for (int i = 0; i < frontier.getVertexCount(); ++i) {
            BlockPos v = frontier.getVertex(i);
            if (vertex.getX() == v.getX() && vertex.getZ() == v.getZ()) {
                return true;
            }
        }

        return false;
    }

    public static void setjmAPI(IClientAPI newJmAPI) {
        jmAPI = newJmAPI;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            ItemStack itemMainhand = player.getItemBySlot(EquipmentSlotType.MAINHAND);
            ItemStack itemOffhand = player.getItemBySlot(EquipmentSlotType.OFFHAND);

            if (itemMainhand != bookItemInHand && itemOffhand != bookItemInHand) {
                bookItemInHand = null;

                if (!itemMainhand.isEmpty() && (itemMainhand.getItem() == MapFrontiers.frontierBook
                        || itemMainhand.getItem() == MapFrontiers.personalFrontierBook)) {
                    if (itemMainhand.hasTag()) {
                        CompoundNBT nbt = itemMainhand.getTag();
                        if (nbt != null && nbt.contains("Dimension")
                                && nbt.getString("Dimension").equals(player.level.dimension().location().toString())) {
                            bookItemInHand = itemMainhand;
                        }
                    }
                }

                if (bookItemInHand == null && !itemOffhand.isEmpty() && (itemOffhand.getItem() == MapFrontiers.frontierBook
                        || itemMainhand.getItem() == MapFrontiers.personalFrontierBook)) {
                    if (itemOffhand.hasTag()) {
                        CompoundNBT nbt = itemOffhand.getTag();
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

    public static void openGUIFrontierBook(RegistryKey<World> dimension, boolean personal) {
        if (frontiersOverlayManager == null || settingsProfile == null) {
            return;
        }

        ItemStack mainhand = Minecraft.getInstance().player.getItemBySlot(EquipmentSlotType.MAINHAND);
        ItemStack offhand = Minecraft.getInstance().player.getItemBySlot(EquipmentSlotType.OFFHAND);
        ItemStack heldBanner = null;

        if (mainhand.getItem() instanceof BannerItem) {
            heldBanner = mainhand;
        } else if (offhand.getItem() instanceof BannerItem) {
            heldBanner = offhand;
        }

        RegistryKey<World> currentDimension = Minecraft.getInstance().player.level.dimension();

        if (personal && settingsProfile.personalFrontier == SettingsProfile.State.Enabled) {
            Minecraft.getInstance().setScreen(
                    new GuiFrontierBook(personalFrontiersOverlayManager, personal, currentDimension, dimension, heldBanner));
        } else {
            Minecraft.getInstance().setScreen(
                    new GuiFrontierBook(frontiersOverlayManager, personal, currentDimension, dimension, heldBanner));
        }
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

    public static void setSettingsProfile(SettingsProfile newSettingsProfile) {
        settingsProfile = newSettingsProfile;
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

    public static void frontierChanged() {
        if (guiHUD != null) {
            guiHUD.frontierChanged();
        }
    }
}
