package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.client.Config;
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
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

//@Mod.EventBusSubscriber
@Mod(MapFrontiersForge.MODID)
public class MapFrontiersForge extends MapFrontiers {
    public MapFrontiersForge(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);

        FMLCommonSetupEvent.getBus(context.getModBusGroup()).addListener(MapFrontiersForge::commonSetup);
        ModConfigEvent.Loading.getBus(context.getModBusGroup()).addListener(MapFrontiersForge::onModConfigEvent);

        RegisterCommandsEvent.BUS.addListener(MapFrontiersForge::registerCommands);
        ServerStartingEvent.BUS.addListener(MapFrontiersForge::serverStarting);
        ServerStoppingEvent.BUS.addListener(MapFrontiersForge::serverStopping);
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(MapFrontiersForge::playerLoggedIn);
        TickEvent.ServerTickEvent.Post.BUS.addListener(MapFrontiersForge::onServerTick);

        if (FMLEnvironment.dist.isClient()) {
            FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(MapFrontiersClientForge::clientSetup);

            LivingEvent.LivingTickEvent.BUS.addListener(MapFrontiersClientForge::livingUpdateEvent);
            TickEvent.ClientTickEvent.Pre.BUS.addListener(MapFrontiersClientForge::onRenderTick);
            AddGuiOverlayLayersEvent.BUS.addListener(MapFrontiersClientForge::addGuiOverlayLayersEvent);
            ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(MapFrontiersClientForge::clientConnectedToServer);
            ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(MapFrontiersClientForge::clientDisconnectionFromServer);
            InputEvent.MouseButton.Pre.BUS.addListener(MapFrontiersClientForge::mouseEvent);
            RegisterKeyMappingsEvent.BUS.addListener(MapFrontiersClientForge::registerKeyMappingsEvent);
            RegisterClientCommandsEvent.BUS.addListener(MapFrontiersClientForge::onRegisterClientCommands);
            ClientChatReceivedEvent.BUS.addListener(MapFrontiersClientForge::onClientChat);
        }
    }

    public static void commonSetup(FMLCommonSetupEvent event) {
        init();
        LOGGER.info("Forge commonSetup done");
    }

    @SubscribeEvent
    public static void onModConfigEvent(ModConfigEvent.Loading configEvent) {
        if (configEvent.getConfig().getModId().equals(MapFrontiersForge.MODID) && configEvent.getConfig().getType() == ModConfig.Type.CLIENT) {
            Config.bakeConfig();
        }
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        CommandAccept.register(event.getDispatcher());
    }

    public static void serverStarting(ServerStartingEvent event) {
        ServerGlobalEvents.postServerStartingEvent(event.getServer());
    }

    public static void serverStopping(ServerStoppingEvent event) {
        ServerGlobalEvents.postServerStoppingEvent(event.getServer());
    }

    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        ServerGlobalEvents.postPlayerJoinedEvent(player.level().getServer(), player);
    }

    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        ServerGlobalEvents.postServerTickEvent(event.server());
    }
}

