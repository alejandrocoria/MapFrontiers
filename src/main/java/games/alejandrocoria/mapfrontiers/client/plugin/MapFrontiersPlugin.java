package games.alejandrocoria.mapfrontiers.client.plugin;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFullscreenMap;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.Context;
import journeymap.client.api.display.ModPopupMenu;
import journeymap.client.api.display.ThemeButtonDisplay;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.event.DisplayUpdateEvent;
import journeymap.client.api.event.FullscreenMapEvent;
import journeymap.client.api.event.forge.FullscreenDisplayEvent;
import journeymap.client.api.event.forge.PopupMenuEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;

@ParametersAreNonnullByDefault
@journeymap.client.api.ClientPlugin
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MapFrontiers.MODID)
@OnlyIn(Dist.CLIENT)
public class MapFrontiersPlugin implements IClientPlugin {
    private static IClientAPI jmAPI;
    private static GuiFullscreenMap fullscreenMap;

    @Override
    public void initialize(final IClientAPI jmAPI) {
        MapFrontiersPlugin.jmAPI = jmAPI;
        ClientProxy.setjmAPI(jmAPI);
        jmAPI.subscribe(MapFrontiers.MODID, EnumSet.of(
                ClientEvent.Type.MAP_CLICKED,
                ClientEvent.Type.MAP_DRAGGED,
                ClientEvent.Type.DISPLAY_UPDATE));
    }

    @Override
    public String getModId() {
        return MapFrontiers.MODID;
    }

    @Override
    public void onEvent(ClientEvent event) {
        switch (event.type) {
            case MAP_CLICKED:
                if (fullscreenMap != null) {
                    FullscreenMapEvent.ClickEvent clickEvent = (FullscreenMapEvent.ClickEvent)event;
                    if (clickEvent.getStage() == FullscreenMapEvent.Stage.POST) {
                        fullscreenMap.mapClicked(clickEvent.dimension, clickEvent.getLocation());
                    }
                }
                break;
            case MAP_DRAGGED:
                if (fullscreenMap != null) {
                    FullscreenMapEvent.MouseDraggedEvent mouseDraggedEvent = (FullscreenMapEvent.MouseDraggedEvent) event;
                    if (mouseDraggedEvent.getStage() == FullscreenMapEvent.Stage.PRE) {
                        boolean moved = fullscreenMap.mapDragged(mouseDraggedEvent.dimension, mouseDraggedEvent.getLocation());
                        if (moved) {
                            mouseDraggedEvent.cancel();
                        }
                    }
                }
                break;
            case DISPLAY_UPDATE:
                if (fullscreenMap != null) {
                    DisplayUpdateEvent displayEvent = (DisplayUpdateEvent)event;
                    if (displayEvent.uiState.ui == Context.UI.Fullscreen) {
                        if (displayEvent.uiState.active) {
                            fullscreenMap.updatebuttons();
                        } else {
                            fullscreenMap.stopEditing();
                            fullscreenMap.close();
                            fullscreenMap = null;
                        }
                    }
                }
                break;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFullscreenAddonButton(FullscreenDisplayEvent.AddonButtonDisplayEvent event) {
        if (fullscreenMap == null) {
            fullscreenMap = new GuiFullscreenMap(jmAPI);
        }

        ThemeButtonDisplay buttonDisplay = event.getThemeButtonDisplay();
        fullscreenMap.addButtons(buttonDisplay);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFullscreenpopupMenu(PopupMenuEvent.FullscreenPopupMenuEvent event) {
        if (fullscreenMap != null) {
            ModPopupMenu popupMenu = event.getPopupMenu();
            fullscreenMap.addPopupMenu(popupMenu);
        }
    }

    public static boolean isEditing() {
        return fullscreenMap != null && fullscreenMap.isEditing();
    }
}
