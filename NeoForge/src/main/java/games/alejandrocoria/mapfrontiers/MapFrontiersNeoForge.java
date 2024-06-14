package games.alejandrocoria.mapfrontiers;

import fuzs.forgeconfigapiport.neoforge.api.forge.v4.ForgeConfigRegistry;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClientNeoForge;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.event.EventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(MapFrontiersNeoForge.MODID)
public class MapFrontiersNeoForge extends MapFrontiers {
    public MapFrontiersNeoForge(IEventBus eventBus) {
        init();

        eventBus.addListener((FMLConstructModEvent event) -> ForgeConfigRegistry.INSTANCE.register(MapFrontiersNeoForge.MODID, ModConfig.Type.CLIENT, Config.CLIENT_SPEC));
        eventBus.addListener((FMLClientSetupEvent event) -> MapFrontiersClientNeoForge.clientSetup(event, eventBus));
        NeoForge.EVENT_BUS.addListener(MapFrontiersNeoForge::registerCommands);
        NeoForge.EVENT_BUS.addListener(MapFrontiersNeoForge::serverStarting);
        NeoForge.EVENT_BUS.addListener(MapFrontiersNeoForge::serverStopping);
        NeoForge.EVENT_BUS.addListener(MapFrontiersNeoForge::playerLoggedIn);
        NeoForge.EVENT_BUS.addListener(MapFrontiersNeoForge::onServerTick);

        LOGGER.info("NeoForge commonSetup done");
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        EventHandler.postCommandRegistrationEvent(event.getDispatcher());
    }

    public static void serverStarting(ServerStartingEvent event) {
        EventHandler.postServerStartingEvent(event.getServer());
    }

    public static void serverStopping(ServerStoppingEvent event) {
        EventHandler.postServerStoppingEvent(event.getServer());
    }

    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        EventHandler.postPlayerJoinedEvent(player.server, player);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        EventHandler.postServerTickEvent(event.getServer());
    }
}
