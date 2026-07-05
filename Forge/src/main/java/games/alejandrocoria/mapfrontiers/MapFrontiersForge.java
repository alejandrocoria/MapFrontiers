package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClientForge;
import games.alejandrocoria.mapfrontiers.server.command.CommandAccept;
import games.alejandrocoria.mapfrontiers.server.event.ServerGlobalEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

//@Mod.EventBusSubscriber
@Mod(MapFrontiersForge.MODID)
public class MapFrontiersForge extends MapFrontiers {
    public MapFrontiersForge(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(MapFrontiersForge::onCommonSetup);

        MinecraftForge.EVENT_BUS.addListener(MapFrontiersForge::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(MapFrontiersForge::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(MapFrontiersForge::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(MapFrontiersForge::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(MapFrontiersForge::onServerTick);

        if (FMLEnvironment.dist.isClient()) {
            context.getModEventBus().addListener(MapFrontiersClientForge::onClientSetup);
            context.getModEventBus().addListener(MapFrontiersClientForge::onRegisterKeyMappings);
            context.getModEventBus().addListener(MapFrontiersClientForge::onRegisterGuiOverlays);

            MinecraftForge.EVENT_BUS.addListener(MapFrontiersClientForge::onLivingTick);
            MinecraftForge.EVENT_BUS.addListener(MapFrontiersClientForge::onClientTickPre);
            MinecraftForge.EVENT_BUS.addListener(MapFrontiersClientForge::onClientConnectedToServer);
            MinecraftForge.EVENT_BUS.addListener(MapFrontiersClientForge::onClientDisconnectedFromServer);
            MinecraftForge.EVENT_BUS.addListener(MapFrontiersClientForge::onMouseButtonPre);
            MinecraftForge.EVENT_BUS.addListener(MapFrontiersClientForge::onRegisterClientCommands);
            MinecraftForge.EVENT_BUS.addListener(MapFrontiersClientForge::onClientChat);
        }
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        init();
        LOGGER.info("Forge commonSetup done");
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandAccept.register(event.getDispatcher());
    }

    public static void onServerStarted(ServerStartedEvent event) {
        ServerGlobalEvents.postServerStartingEvent(event.getServer());
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        ServerGlobalEvents.postServerStoppingEvent(event.getServer());
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        ServerGlobalEvents.postPlayerJoinedEvent(player.level().getServer(), player);
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ServerGlobalEvents.postServerTickEvent(event.getServer());
        }
    }
}
