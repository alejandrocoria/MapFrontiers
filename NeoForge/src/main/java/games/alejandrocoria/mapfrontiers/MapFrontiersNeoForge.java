package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClientNeoForge;
import games.alejandrocoria.mapfrontiers.server.command.CommandAccept;
import games.alejandrocoria.mapfrontiers.server.event.ServerGlobalEvents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
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

        eventBus.addListener((FMLClientSetupEvent event) -> MapFrontiersClientNeoForge.onClientSetup(event, eventBus));
        NeoForge.EVENT_BUS.addListener(MapFrontiersNeoForge::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(MapFrontiersNeoForge::onServerStarting);
        NeoForge.EVENT_BUS.addListener(MapFrontiersNeoForge::onServerStopping);
        NeoForge.EVENT_BUS.addListener(MapFrontiersNeoForge::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(MapFrontiersNeoForge::onServerTick);

        LOGGER.info("NeoForge commonSetup done");
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandAccept.register(event.getDispatcher());
    }

    public static void onServerStarting(ServerStartingEvent event) {
        ServerGlobalEvents.postServerStartingEvent(event.getServer());
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        ServerGlobalEvents.postServerStoppingEvent(event.getServer());
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        ServerGlobalEvents.postPlayerJoinedEvent(player.level().getServer(), player);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerGlobalEvents.postServerTickEvent(event.getServer());
    }
}
