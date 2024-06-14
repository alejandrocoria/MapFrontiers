package games.alejandrocoria.mapfrontiers.client;

import com.mojang.blaze3d.platform.InputConstants;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.MapFrontiersForge;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.common.Config;
import journeymap.client.api.display.ModPopupMenu;
import journeymap.client.api.display.ThemeButtonDisplay;
import journeymap.client.api.event.forge.FullscreenDisplayEvent;
import journeymap.client.api.event.forge.PopupMenuEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MapFrontiersForge.MODID)
public class MapFrontiersClientForge extends MapFrontiersClient {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        openSettingsKey = new KeyMapping("mapfrontiers.key.open_settings", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "mapfrontiers.key.category");

        init();

        MapFrontiersForge.LOGGER.info("Forge clientSetup done");
    }

    @SubscribeEvent
    public static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
        event.register(openSettingsKey);
    }

    @SubscribeEvent
    public static void livingUpdateEvent(LivingEvent.LivingTickEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (event.getEntity() == client.player) {
            Player player = (Player) event.getEntity();
            ClientEventHandler.postPlayerTickEvent(client, player);
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ClientEventHandler.postClientTickEvent(Minecraft.getInstance());
        }
    }

    @SubscribeEvent
    public static void clientConnectedToServer(LoggingIn event) {
        ClientEventHandler.postClientConnectedEvent();
    }

    @SubscribeEvent
    public static void clientDisconnectionFromServer(LoggingOut event) {
        ClientEventHandler.postClientDisconnectedEvent();
    }

    @SubscribeEvent
    public static void mouseEvent(InputEvent.MouseButton event) {
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            ClientEventHandler.postMouseReleaseEvent(event.getButton());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFullscreenAddonButton(FullscreenDisplayEvent.AddonButtonDisplayEvent event) {
        ThemeButtonDisplay buttonDisplay = event.getThemeButtonDisplay();
        ClientEventHandler.postAddonButtonDisplayEvent(buttonDisplay);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFullscreenpopupMenu(PopupMenuEvent.FullscreenPopupMenuEvent event) {
        ModPopupMenu popupMenu = event.getPopupMenu();
        ClientEventHandler.postFullscreenPopupMenuEvent(popupMenu);
    }

    @Mod.EventBusSubscriber(modid = MapFrontiers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ConfigEventHandler {
        @SubscribeEvent
        public static void onModConfigEvent(ModConfigEvent.Loading configEvent) {
            if (configEvent.getConfig().getModId().equals(MapFrontiersForge.MODID) && configEvent.getConfig().getType() == ModConfig.Type.CLIENT) {
                Config.bakeConfig();
            }
        }
    }
}
