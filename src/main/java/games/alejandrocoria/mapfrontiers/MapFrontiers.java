package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.command.CommandAccept;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.OpEntry;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

@Mod.EventBusSubscriber
@Mod(MapFrontiers.MODID)
public class MapFrontiers {
    public static final String MODID = "mapfrontiers";
    public static final String VERSION = "1.18.2-2.1.4";
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

    @SubscribeEvent
    public static void onMissingMappingEventItems(RegistryEvent.MissingMappings<Item> event) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (mapping.key.equals(new ResourceLocation(MapFrontiers.MODID, "frontier_book"))) {
                mapping.ignore();
            } else if (mapping.key.equals(new ResourceLocation(MapFrontiers.MODID, "personal_frontier_book"))) {
                mapping.ignore();
            }
        }
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
                PacketHandler.sendTo(new PacketFrontier(frontier), (ServerPlayerEntity) event.getPlayer());
            }
        }

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllPersonalFrontiers(new SettingsUser(event.getPlayer()))
                .values()) {
            for (FrontierData frontier : frontiers) {
                PacketHandler.sendTo(new PacketFrontier(frontier), (ServerPlayerEntity) event.getPlayer());
            }
        }

        PacketHandler.sendTo(new PacketSettingsProfile(frontiersManager.getSettings().getProfile(event.getPlayer())),
                (ServerPlayerEntity) event.getPlayer());
    }

    public static FrontiersManager getFrontiersManager() {
        return frontiersManager;
    }

    public static boolean isOPorHost(PlayerEntity player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        OpEntry opEntry = server.getPlayerList().getOps().get(player.getGameProfile());

        if (opEntry != null) {
            return true;
        }

        if (server.isSingleplayer()) {
            return server.isSingleplayerOwner(player.getGameProfile()) || player.isCreative();
        }

        return false;
    }
}
