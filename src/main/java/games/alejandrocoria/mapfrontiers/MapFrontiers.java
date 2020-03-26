package games.alejandrocoria.mapfrontiers;

import org.apache.logging.log4j.Logger;

import games.alejandrocoria.mapfrontiers.common.CommonProxy;
import games.alejandrocoria.mapfrontiers.common.item.ItemFrontierBook;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber
@Mod(modid = MapFrontiers.MODID, version = MapFrontiers.VERSION, dependencies = "required-after:journeymap", updateJSON = "https://alejandrocoria.games/projects/MapFrontiers/update.json")
public class MapFrontiers {
    public static final String MODID = "mapfrontiers";
    public static final String VERSION = "@VERSION@";
    public static Logger LOGGER;

    @Mod.Instance(MapFrontiers.MODID)
    public static MapFrontiers instance;

    @SidedProxy(clientSide = "games.alejandrocoria.mapfrontiers.client.ClientProxy", serverSide = "games.alejandrocoria.mapfrontiers.server.ServerProxy")
    public static CommonProxy proxy;

    @ObjectHolder("mapfrontiers:frontier_book")
    public static ItemFrontierBook frontierBook = new ItemFrontierBook();

    @SideOnly(Side.CLIENT)
    public static void initModels() {
        frontierBook.initModel();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(frontierBook);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        proxy.preInit(event);

        LOGGER.info("MapFrontiers preInit done");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        PacketHandler.init();
        proxy.init(event);
        LOGGER.info("MapFrontiers init done");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
        LOGGER.info("MapFrontiers postInit done");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
        LOGGER.info("MapFrontiers serverStarting done");
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
        LOGGER.info("MapFrontiers serverStopping done");
    }
}
