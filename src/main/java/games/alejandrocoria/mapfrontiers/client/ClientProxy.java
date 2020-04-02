package games.alejandrocoria.mapfrontiers.client;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import games.alejandrocoria.mapfrontiers.common.CommonProxy;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import journeymap.client.api.IClientAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
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

    // Note: copied from CommonProxy, needs an abstraction
    @Override
    public BlockPos snapVertex(BlockPos vertex, int snapDistance, FrontierData owner) {
        float snapDistanceSq = snapDistance * snapDistance;
        BlockPos closest = new BlockPos(vertex.getX(), 70, vertex.getZ());
        double closestDistance = Double.MAX_VALUE;
        for (FrontierData frontier : frontiersOverlayManager.getAllFrontiers(owner.getDimension())) {
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
    public void clientConnectedToServer(ClientConnectedToServerEvent event) {
        if (jmAPI != null) {
            if (frontiersOverlayManager != null) {
                frontiersOverlayManager.removeAllOverlays();
            }
            frontiersOverlayManager = new FrontiersOverlayManager(jmAPI);
        }
    }

    @SubscribeEvent
    public void clientDisconnectionFromServer(ClientDisconnectionFromServerEvent event) {
        frontiersOverlayManager.removeAllOverlays();
        frontiersOverlayManager = null;
    }

    public void openGUIFrontierBook(int dimension) {
        if (frontiersOverlayManager == null) {
            return;
        }

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