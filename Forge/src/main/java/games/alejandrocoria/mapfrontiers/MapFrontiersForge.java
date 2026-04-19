package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClientForge;
import games.alejandrocoria.mapfrontiers.server.command.CommandAccept;
import games.alejandrocoria.mapfrontiers.server.event.ServerGlobalEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

//@Mod.EventBusSubscriber
@Mod(MapFrontiersForge.MODID)
public class MapFrontiersForge extends MapFrontiers {
    public MapFrontiersForge(FMLJavaModLoadingContext context) {
        FMLCommonSetupEvent.getBus(context.getModBusGroup()).addListener(MapFrontiersForge::onCommonSetup);

        RegisterCommandsEvent.BUS.addListener(MapFrontiersForge::onRegisterCommands);
        ServerStartingEvent.BUS.addListener(MapFrontiersForge::onServerStarting);
        ServerStoppingEvent.BUS.addListener(MapFrontiersForge::onServerStopping);
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(MapFrontiersForge::onPlayerLoggedIn);
        TickEvent.ServerTickEvent.Post.BUS.addListener(MapFrontiersForge::onServerTick);

        if (FMLEnvironment.dist.isClient()) {
            FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(MapFrontiersClientForge::onClientSetup);

            LivingEvent.LivingTickEvent.BUS.addListener(MapFrontiersClientForge::onLivingTick);
            TickEvent.ClientTickEvent.Pre.BUS.addListener(MapFrontiersClientForge::onClientTickPre);
            AddGuiOverlayLayersEvent.BUS.addListener(MapFrontiersClientForge::onAddGuiOverlayLayers);
            ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(MapFrontiersClientForge::onClientConnectedToServer);
            ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(MapFrontiersClientForge::onClientDisconnectedFromServer);
            InputEvent.MouseButton.Pre.BUS.addListener(MapFrontiersClientForge::onMouseButtonPre);
            RegisterKeyMappingsEvent.BUS.addListener(MapFrontiersClientForge::onRegisterKeyMappings);
            RegisterClientCommandsEvent.BUS.addListener(MapFrontiersClientForge::onRegisterClientCommands);
            ClientChatReceivedEvent.BUS.addListener(MapFrontiersClientForge::onClientChat);
        }
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        init();
        LOGGER.info("Forge commonSetup done");
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

    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        ServerGlobalEvents.postServerTickEvent(event.server());
    }
}
