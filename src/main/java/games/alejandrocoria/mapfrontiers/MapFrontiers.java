package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.command.CommandAccept;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontiers;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

@Mod.EventBusSubscriber
@Mod(MapFrontiers.MODID)
public class MapFrontiers {
    public static final String MODID = "mapfrontiers";
    public static final String VERSION = "1.19.2-2.5.1";
    public static Logger LOGGER;

    private static FrontiersManager frontiersManager;

    public MapFrontiers() {
        LOGGER = LogManager.getLogger("MapFrontiers");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigData.CLIENT_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(MapFrontiers::commonSetup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> MapFrontiers::addListenerClientSetup);
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        PacketHandler.init();
        LOGGER.info("commonSetup done");
    }

    @OnlyIn(Dist.CLIENT)
    public static void addListenerClientSetup() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientProxy::clientSetup);
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent event) {
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
    public static void serverStopping(ServerStoppingEvent event) {
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

        ServerPlayer player = (ServerPlayer) event.getEntity();

        PacketHandler.sendTo(new PacketSettingsProfile(frontiersManager.getSettings().getProfile(event.getEntity())), player);

        PacketFrontiers packetFrontiers = new PacketFrontiers();
        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllGlobalFrontiers().values()) {
            packetFrontiers.addGlobalFrontiers(frontiers);
        }

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllPersonalFrontiers(new SettingsUser(event.getEntity())).values()) {
            packetFrontiers.addPersonalFrontiers(frontiers);
        }

        PacketHandler.sendTo(packetFrontiers, player);
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
