package games.alejandrocoria.mapfrontiers.client;

import com.mojang.blaze3d.platform.InputConstants;
import games.alejandrocoria.mapfrontiers.MapFrontiersFabric;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MapFrontiersClientFabric extends MapFrontiersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "mapfrontiers.key.open_settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "mapfrontiers.key.category"
        ));

        ClientTickEvents.START_CLIENT_TICK.register(ClientEventHandler::postClientTickEvent);
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientEventHandler.postPlayerTickEvent(client, client.player));
        HudRenderCallback.EVENT.register(ClientEventHandler::postHudRenderEvent);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientEventHandler.postClientConnectedEvent());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientEventHandler.postClientDisconnectedEvent());
        ScreenEvents.BEFORE_INIT.register((client, theScreen, scaledWidth, scaledHeight) -> {
            ScreenMouseEvents.beforeMouseRelease(theScreen).register((screen, mouseX, mouseY, button) -> ClientEventHandler.postMouseReleaseEvent(button));
        });

        init();

        MapFrontiersFabric.LOGGER.info("Fabric onInitializeClient done");
    }
}
