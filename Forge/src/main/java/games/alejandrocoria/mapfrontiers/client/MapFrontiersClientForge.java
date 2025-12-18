package games.alejandrocoria.mapfrontiers.client;

import com.mojang.blaze3d.platform.InputConstants;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.MapFrontiersForge;
import games.alejandrocoria.mapfrontiers.client.command.ClientCommandAccept;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MapFrontiersClientForge extends MapFrontiersClient {
    public static void clientSetup(FMLClientSetupEvent event) {
        init();

        MapFrontiersForge.LOGGER.info("Forge clientSetup done");
    }

    public static void livingUpdateEvent(LivingEvent.LivingTickEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (event.getEntity() == client.player) {
            Player player = (Player) event.getEntity();
            ClientEventHandler.postPlayerTickEvent(client, player);
        }
    }

    public static void onRenderTick(TickEvent.ClientTickEvent.Pre event) {
        ClientEventHandler.postClientTickEvent(Minecraft.getInstance());
    }

    public static void addGuiOverlayLayersEvent(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "hud"), ClientEventHandler::postHudRenderEvent);
    }

    public static void clientConnectedToServer(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientEventHandler.postClientConnectedEvent();
    }

    public static void clientDisconnectionFromServer(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientEventHandler.postClientDisconnectedEvent();
    }

    public static void mouseEvent(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            ClientEventHandler.postMouseReleaseEvent(event.getButton());
        }
    }

    public static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
        openSettingsKey = new KeyMapping("mapfrontiers.key.open_settings", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, MapFrontiersClient.registerKeyMappingCategory(), 0);
        event.register(openSettingsKey);
    }

    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ClientCommandAccept.register(event.getDispatcher());
    }

    public static void onClientChat(ClientChatReceivedEvent event) {
        boolean cancel = ChatFrontiers.receiveFrontierFromChat(event.getMessage(), event.getSender());
        if (cancel) {
            // Cannot be canceled
            event.setMessage(Component.empty());
        }
    }
}
