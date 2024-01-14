package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClientNeoForge;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.event.EventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod.EventBusSubscriber
@Mod(MapFrontiersNeoForge.MODID)
public class MapFrontiersNeoForge extends MapFrontiers {
    public MapFrontiersNeoForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, (IConfigSpec<?>) Config.CLIENT_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(MapFrontiersNeoForge::commonSetup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> MapFrontiersNeoForge::addListenerClientSetup);
    }

    public static void commonSetup(FMLCommonSetupEvent event) {
        init();
        LOGGER.info("NeoForge commonSetup done");
    }

    @OnlyIn(Dist.CLIENT)
    public static void addListenerClientSetup() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(MapFrontiersClientNeoForge::clientSetup);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        EventHandler.postCommandRegistrationEvent(event.getDispatcher());
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent event) {
        EventHandler.postServerStartingEvent(event.getServer());
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        EventHandler.postServerStoppingEvent(event.getServer());
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        EventHandler.postPlayerJoinedEvent(player.server, player);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            EventHandler.postServerTickEvent(event.getServer());
        }
    }
}
