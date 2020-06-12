package games.alejandrocoria.mapfrontiers.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.lwjgl.input.Keyboard;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;
import games.alejandrocoria.mapfrontiers.client.gui.GuiHUD;
import games.alejandrocoria.mapfrontiers.common.CommonProxy;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import journeymap.client.api.IClientAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = MapFrontiers.MODID)
public class ClientProxy extends CommonProxy {
    private IClientAPI jmAPI;
    private FrontiersOverlayManager frontiersOverlayManager;
    private FrontiersOverlayManager personalFrontiersOverlayManager;
    private SettingsProfile settingsProfile;

    private KeyBinding openSettingsKey;
    private GuiHUD guiHUD;

    private static ItemStack bookItemInHand;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        Minecraft.getMinecraft().getFramebuffer().enableStencil();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(Sounds.class);

        openSettingsKey = new KeyBinding("mapfrontiers.key.open_settings", KeyConflictContext.IN_GAME, Keyboard.KEY_F8,
                "mapfrontiers.key.category");
        ClientRegistry.registerKeyBinding(openSettingsKey);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        MinecraftForge.EVENT_BUS.register(FrontierOverlay.class);
        MinecraftForge.EVENT_BUS.register(FrontiersOverlayManager.class);
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        if (Minecraft.getMinecraft().isIntegratedServerRunning()) {
            super.serverStarting(event);
        }
    }

    @Override
    public void serverStopping(FMLServerStoppingEvent event) {
        if (Minecraft.getMinecraft().isIntegratedServerRunning()) {
            super.serverStopping(event);
        }
    }

    @SubscribeEvent()
    public void onEvent(KeyInputEvent event) {
        if (openSettingsKey.isPressed()) {
            if (frontiersOverlayManager == null) {
                return;
            }

            if (settingsProfile == null) {
                return;
            }

            Minecraft.getMinecraft().displayGuiScreen(new GuiFrontierSettings(settingsProfile));
        }
    }

    public BlockPos snapVertex(BlockPos vertex, int snapDistance, int dimension, FrontierData owner) {
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
                double distance = v2.distanceSq(closest);
                if (distance < snapDistanceSq && distance < closestDistance) {
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
                double distance = v2.distanceSq(closest);
                if (distance < snapDistanceSq && distance < closestDistance) {
                    closestDistance = distance;
                    closest = v2;
                }
            }
        }

        return closest;
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        MapFrontiers.initModels();
    }

    public void setjmAPI(IClientAPI jmAPI) {
        this.jmAPI = jmAPI;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null) {
                return;
            }

            ItemStack itemMainhand = player.getHeldItemMainhand();
            ItemStack itemOffhand = player.getHeldItemOffhand();

            if (itemMainhand != bookItemInHand && itemOffhand != bookItemInHand) {
                bookItemInHand = null;

                if (itemMainhand != null && (itemMainhand.getItem() == MapFrontiers.frontierBook
                        || itemMainhand.getItem() == MapFrontiers.personalFrontierBook)) {
                    if (itemMainhand.hasTagCompound()) {
                        NBTTagCompound nbt = itemMainhand.getTagCompound();
                        if (nbt.hasKey("Dimension") && nbt.getInteger("Dimension") == player.dimension) {
                            bookItemInHand = itemMainhand;
                        }
                    }
                }

                if (bookItemInHand == null && itemOffhand != null && (itemOffhand.getItem() == MapFrontiers.frontierBook
                        || itemMainhand.getItem() == MapFrontiers.personalFrontierBook)) {
                    if (itemOffhand.hasTagCompound()) {
                        NBTTagCompound nbt = itemOffhand.getTagCompound();
                        if (nbt.hasKey("Dimension") && nbt.getInteger("Dimension") == player.dimension) {
                            bookItemInHand = itemOffhand;
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void clientConnectedToServer(ClientConnectedToServerEvent event) {
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
    public void clientDisconnectionFromServer(ClientDisconnectionFromServerEvent event) {
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

    public void openGUIFrontierBook(int dimension, boolean personal) {
        if (frontiersOverlayManager == null || settingsProfile == null) {
            return;
        }

        ItemStack mainhand = Minecraft.getMinecraft().player.getHeldItemMainhand();
        ItemStack offhand = Minecraft.getMinecraft().player.getHeldItemOffhand();
        ItemStack heldBanner = null;

        if (mainhand.getItem() instanceof ItemBanner) {
            heldBanner = mainhand;
        } else if (offhand.getItem() instanceof ItemBanner) {
            heldBanner = offhand;
        }

        int currentDimension = Minecraft.getMinecraft().player.dimension;

        if (personal && settingsProfile.personalFrontier == SettingsProfile.State.Enabled) {
            Minecraft.getMinecraft().displayGuiScreen(
                    new GuiFrontierBook(personalFrontiersOverlayManager, personal, currentDimension, dimension, heldBanner));
        } else {
            Minecraft.getMinecraft().displayGuiScreen(
                    new GuiFrontierBook(frontiersOverlayManager, personal, currentDimension, dimension, heldBanner));
        }
    }

    public static boolean hasBookItemInHand() {
        return bookItemInHand != null;
    }

    public FrontiersOverlayManager getFrontiersOverlayManager(boolean personal) {
        if (personal) {
            return personalFrontiersOverlayManager;
        } else {
            return frontiersOverlayManager;
        }
    }

    public void setSettingsProfile(SettingsProfile settingsProfile) {
        this.settingsProfile = settingsProfile;
    }

    public SettingsProfile getSettingsProfile() {
        return settingsProfile;
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(MapFrontiers.MODID)) {
            configUpdated();
        }
    }

    @Override
    public void configUpdated() {
        ConfigManager.sync(MapFrontiers.MODID, Config.Type.INSTANCE);

        if (frontiersOverlayManager != null) {
            frontiersOverlayManager.updateAllOverlays();
            personalFrontiersOverlayManager.updateAllOverlays();
        }

        if (guiHUD != null) {
            guiHUD.configUpdated();
        }
    }

    @Override
    public void frontierChanged() {
        if (guiHUD != null) {
            guiHUD.frontierChanged();
        }
    }

    @Override
    public boolean isOPorHost(EntityPlayer player) {
        if (Minecraft.getMinecraft().isIntegratedServerRunning()) {
            return super.isOPorHost(player);
        }

        // @Note: This function does not have to be called on the remote client.
        // The information is not here.

        MapFrontiers.LOGGER.error("ClientProxy.isOPorHost called on the remote client. This should not happen!");
        return false;
    }
}