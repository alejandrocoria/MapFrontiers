package games.alejandrocoria.mapfrontiers;

import fuzs.forgeconfigapiport.fabric.api.forge.v4.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeModConfigEvents;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.event.EventHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.neoforged.fml.config.ModConfig;

public class MapFrontiersFabric extends MapFrontiers implements ModInitializer {
    public MapFrontiersFabric() {
    }

    @Override
    public void onInitialize() {
        NeoForgeModConfigEvents.loading(MapFrontiers.MODID).register(config -> Config.bakeConfig());
        ForgeConfigRegistry.INSTANCE.register(MapFrontiersFabric.MODID, ModConfig.Type.CLIENT, Config.CLIENT_SPEC);

        init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> EventHandler.postCommandRegistrationEvent(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(EventHandler::postServerStartingEvent);
        ServerLifecycleEvents.SERVER_STOPPING.register(EventHandler::postServerStoppingEvent);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> EventHandler.postPlayerJoinedEvent(server, handler.player));
        ServerTickEvents.START_SERVER_TICK.register(EventHandler::postServerTickEvent);

        LOGGER.info("Fabric onInitialize done");
    }
}
