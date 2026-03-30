package games.alejandrocoria.mapfrontiers.client;

import com.mojang.blaze3d.platform.InputConstants;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.MapFrontiersFabric;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MapFrontiersClientFabric extends MapFrontiersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        openSettingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "mapfrontiers.key.open_settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, MapFrontiersClient.registerKeyMappingCategory()
        ));

        ClientTickEvents.START_CLIENT_TICK.register(ClientGlobalEvents::postClientTickEvent);
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientGlobalEvents.postPlayerTickEvent(client, client.player));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "hud"), ClientGlobalEvents::postHudRenderEvent);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientGlobalEvents.postClientConnectedEvent());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientGlobalEvents.postClientDisconnectedEvent());
        ScreenEvents.BEFORE_INIT.register((client, theScreen, scaledWidth, scaledHeight) -> {
            ScreenMouseEvents.beforeMouseRelease(theScreen).register((screen, event) -> ClientGlobalEvents.postMouseReleaseEvent(event.button()));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> FabricClientCommandAccept.register(dispatcher));

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            boolean cancel = ChatFrontiers.receiveFrontierFromChat(message, sender != null ? sender.id() : null);
            return !cancel;
        });

        init();

        MapFrontiersFabric.LOGGER.info("Fabric onInitializeClient done");
    }
}
