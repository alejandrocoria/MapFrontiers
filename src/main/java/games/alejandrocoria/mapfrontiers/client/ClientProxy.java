package games.alejandrocoria.mapfrontiers.client;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import games.alejandrocoria.mapfrontiers.common.CommonProxy;
import journeymap.client.api.IClientAPI;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = MapFrontiers.MODID)
public class ClientProxy extends CommonProxy {
    public IClientAPI jmAPI;
    public FrontiersOverlayManager frontiersOverlayManager;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        Minecraft.getMinecraft().getFramebuffer().enableStencil();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        MinecraftForge.EVENT_BUS.register(FrontierOverlay.class);
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

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        MapFrontiers.initModels();
    }

    public void setjmAPI(IClientAPI jmAPI) {
        this.jmAPI = jmAPI;
    }

    @SubscribeEvent
    public void clientConnectedToServer(ClientConnectedToServerEvent event) {
        if (jmAPI != null) {
            frontiersOverlayManager = new FrontiersOverlayManager(jmAPI);
        }
    }

    @SubscribeEvent
    public void clientDisconnectionFromServer(ClientDisconnectionFromServerEvent event) {
        frontiersOverlayManager = null;
    }

    public void openGUIFrontierBook(int dimension) {
        if (frontiersOverlayManager == null) {
            return;
        }

        List<FrontierOverlay> frontiers = frontiersOverlayManager.getAllFrontiers(dimension);

        int currentDimension = Minecraft.getMinecraft().player.dimension;
        Minecraft.getMinecraft().displayGuiScreen(new GuiFrontierBook(frontiersOverlayManager, currentDimension, dimension));
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(MapFrontiers.MODID)) {
            ConfigManager.sync(MapFrontiers.MODID, Config.Type.INSTANCE);
            if (frontiersOverlayManager != null) {
                frontiersOverlayManager.updateAllOverlays();
            }
        }
    }
}