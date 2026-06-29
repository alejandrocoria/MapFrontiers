package games.alejandrocoria.mapfrontiers.client;

import com.mojang.blaze3d.platform.InputConstants;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.MapFrontiersNeoForge;
import games.alejandrocoria.mapfrontiers.client.command.ClientCommandAccept;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.neoforged.neoforge.client.gui.VanillaGuiLayers.EFFECTS;

@ParametersAreNonnullByDefault
public class MapFrontiersClientNeoForge extends MapFrontiersClient {
    public MapFrontiersClientNeoForge() {
    }

    public static void onClientSetup(FMLClientSetupEvent event, IEventBus eventBus) {
        init();
        NeoForge.EVENT_BUS.addListener(MapFrontiersClientNeoForge::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(MapFrontiersClientNeoForge::onClientTickPost);
        NeoForge.EVENT_BUS.addListener(MapFrontiersClientNeoForge::onRenderGuiLayerPre);
        NeoForge.EVENT_BUS.addListener(MapFrontiersClientNeoForge::onClientConnectedToServer);
        NeoForge.EVENT_BUS.addListener(MapFrontiersClientNeoForge::onClientDisconnectedFromServer);
        NeoForge.EVENT_BUS.addListener(MapFrontiersClientNeoForge::onMouseButtonPre);
        NeoForge.EVENT_BUS.addListener(MapFrontiersClientNeoForge::onRegisterClientCommands);

        MapFrontiersNeoForge.LOGGER.info("NeoForge clientSetup done");
    }

    public static void onClientTickPre(ClientTickEvent.Pre event) {
        ClientGlobalEvents.postClientTickEvent(Minecraft.getInstance());
    }

    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            Player player = client.player;
            ClientGlobalEvents.postPlayerTickEvent(client, player);
        }
    }

    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (EFFECTS.equals(event.getName())) {
            ClientGlobalEvents.postHudRenderEvent(event.getGuiGraphics(), event.getPartialTick());
        }
    }

    public static void onClientConnectedToServer(LoggingIn event) {
        ClientGlobalEvents.postClientConnectedEvent();
    }

    public static void onClientDisconnectedFromServer(LoggingOut event) {
        ClientGlobalEvents.postClientDisconnectedEvent();
    }

    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            ClientGlobalEvents.postMouseReleaseEvent(event.getButton());
        }
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ClientCommandAccept.register(event.getDispatcher());
    }

    @EventBusSubscriber(value = Dist.CLIENT, modid = MapFrontiers.MODID)
    public static class KeyMappingsEventHandler {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            openSettingsKey = new KeyMapping("mapfrontiers.key.open_settings", KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, MapFrontiersClient.getKeyMappingCategory());
            event.register(openSettingsKey);
        }
    }
}
