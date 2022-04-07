package games.alejandrocoria.mapfrontiers;

import java.util.ArrayList;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;
import net.minecraftforge.fmlserverevents.FMLServerStartingEvent;
import net.minecraftforge.fmlserverevents.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.command.CommandAccept;
import games.alejandrocoria.mapfrontiers.common.item.ItemFrontierBook;
import games.alejandrocoria.mapfrontiers.common.item.ItemPersonalFrontierBook;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod.EventBusSubscriber
@Mod(MapFrontiers.MODID)
public class MapFrontiers {
    public static final String MODID = "mapfrontiers";
    public static final String VERSION = "1.17.1-2.0.0";
    public static Logger LOGGER;

    private static FrontiersManager frontiersManager;

    public static ItemFrontierBook frontierBook;
    public static ItemPersonalFrontierBook personalFrontierBook;

    public MapFrontiers() {
        LOGGER = LogManager.getLogger("MapFrontiers");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigData.CLIENT_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(MapFrontiers::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(Item.class, MapFrontiers::registerItems);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> MapFrontiers::addListenerClientSetup);
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        PacketHandler.init();
        LOGGER.info("commonSetup done");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        frontierBook = new ItemFrontierBook();
        personalFrontierBook = new ItemPersonalFrontierBook();
        event.getRegistry().register(frontierBook);
        event.getRegistry().register(personalFrontierBook);
        LOGGER.info("registerItems done");
    }

    @OnlyIn(Dist.CLIENT)
    public static void addListenerClientSetup() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientProxy::clientSetup);
    }

    @SubscribeEvent
    public static void serverStarting(FMLServerStartingEvent event) {
        frontiersManager = new FrontiersManager();
        frontiersManager.loadOrCreateData();

        MinecraftForge.EVENT_BUS.register(frontiersManager);
        LOGGER.info("serverStarting done");
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandAccept.register(event.getDispatcher());
        LOGGER.info("registerCommands done");
    }

    @SubscribeEvent
    public static void serverStopping(FMLServerStoppingEvent event) {
        MinecraftForge.EVENT_BUS.unregister(frontiersManager);
        frontiersManager = null;
        LOGGER.info("serverStopping done");
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (frontiersManager == null) {
            return;
        }

        frontiersManager.ensureOwners();

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllGlobalFrontiers().values()) {
            for (FrontierData frontier : frontiers) {
                PacketHandler.sendTo(new PacketFrontier(frontier), (ServerPlayer) event.getPlayer());
            }
        }

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllPersonalFrontiers(new SettingsUser(event.getPlayer()))
                .values()) {
            for (FrontierData frontier : frontiers) {
                PacketHandler.sendTo(new PacketFrontier(frontier), (ServerPlayer) event.getPlayer());
            }
        }

        PacketHandler.sendTo(new PacketSettingsProfile(frontiersManager.getSettings().getProfile(event.getPlayer())),
                (ServerPlayer) event.getPlayer());
    }

    public static FrontiersManager getFrontiersManager() {
        return frontiersManager;
    }

    public static boolean isOPorHost(Player player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        ServerOpListEntry opEntry = server.getPlayerList().getOps().get(player.getGameProfile());

        if (opEntry != null) {
            return true;
        }

        if (server.isSingleplayer()) {
            return server.isSingleplayerOwner(player.getGameProfile()) || player.isCreative();
        }

        return false;
    }
}
