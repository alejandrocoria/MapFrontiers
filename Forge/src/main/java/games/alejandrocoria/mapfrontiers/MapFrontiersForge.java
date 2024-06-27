package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClientForge;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.event.EventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
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

@Mod.EventBusSubscriber
@Mod(MapFrontiersForge.MODID)
public class MapFrontiersForge extends MapFrontiers {
    public MapFrontiersForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(MapFrontiersForge::commonSetup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> MapFrontiersForge::addListenerClientSetup);
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        init();
        LOGGER.info("Forge commonSetup done");
    }

    public static void addListenerClientSetup() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(MapFrontiersClientForge::clientSetup);
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
