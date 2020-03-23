package games.alejandrocoria.mapfrontiers.server;

import java.util.ArrayList;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.IProxy;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(Side.SERVER)
public class ServerProxy implements IProxy {
    private FrontiersManager frontiersManager;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
    }

    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        MapFrontiers.LOGGER.info("serverStarting");
        frontiersManager = new FrontiersManager();
        frontiersManager.loadOrCreateData();
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        MapFrontiers.LOGGER.info("serverStopping");
        frontiersManager = null;
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (frontiersManager == null) {
            return;
        }

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllFrontiers().values()) {
            for (FrontierData frontier : frontiers) {
                PacketHandler.INSTANCE.sendTo(new PacketFrontier(frontier), (EntityPlayerMP) event.player);
            }
        }
    }
}