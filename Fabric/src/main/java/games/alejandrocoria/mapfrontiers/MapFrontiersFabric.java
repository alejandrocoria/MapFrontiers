package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.server.command.CommandAccept;
import games.alejandrocoria.mapfrontiers.server.event.ServerGlobalEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class MapFrontiersFabric extends MapFrontiers implements ModInitializer {
    public MapFrontiersFabric() {
    }

    @Override
    public void onInitialize() {
        init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CommandAccept.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(ServerGlobalEvents::postServerStartingEvent);
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerGlobalEvents::postServerStoppingEvent);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ServerGlobalEvents.postPlayerJoinedEvent(server, handler.player));
        ServerTickEvents.START_SERVER_TICK.register(ServerGlobalEvents::postServerTickEvent);

        LOGGER.info("Fabric onInitialize done");
    }
}
