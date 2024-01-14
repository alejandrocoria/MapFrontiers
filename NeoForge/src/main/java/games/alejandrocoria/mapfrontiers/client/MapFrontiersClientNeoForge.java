package games.alejandrocoria.mapfrontiers.client;

import com.mojang.blaze3d.platform.InputConstants;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.MapFrontiersNeoForge;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.common.Config;
import journeymap.client.api.display.ModPopupMenu;
import journeymap.client.api.display.ThemeButtonDisplay;
import journeymap.client.api.event.neoforge.FullscreenDisplayEvent;
import journeymap.client.api.event.neoforge.PopupMenuEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MapFrontiersNeoForge.MODID)
public class MapFrontiersClientNeoForge extends MapFrontiersClient {
    public MapFrontiersClientNeoForge() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(MapFrontiersClientNeoForge::clientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(MapFrontiersClientNeoForge::registerKeyMappingsEvent);
    }

    public static void clientSetup(FMLClientSetupEvent event) {
        openSettingsKey = new KeyMapping("mapfrontiers.key.open_settings", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "mapfrontiers.key.category");

        init();

        MapFrontiersNeoForge.LOGGER.info("NeoForge clientSetup done");
    }

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
    public static void RenderGameOverlayEvent(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.POTION_ICONS.id())) {
            ClientEventHandler.postHudRenderEvent(event.getGuiGraphics(), event.getPartialTick());
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
    public static void mouseEvent(InputEvent.MouseButton.Post event) {
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
            if (configEvent.getConfig().getModId().equals(MapFrontiersNeoForge.MODID) && configEvent.getConfig().getType() == ModConfig.Type.CLIENT) {
                Config.bakeConfig();
            }
        }
    }
}
