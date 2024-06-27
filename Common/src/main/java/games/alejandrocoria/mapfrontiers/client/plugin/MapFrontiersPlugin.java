package games.alejandrocoria.mapfrontiers.client.plugin;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.FullscreenMap;
import games.alejandrocoria.mapfrontiers.common.Config;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.JourneyMapPlugin;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.event.FullscreenMapEvent;
import journeymap.api.v2.client.fullscreen.ModPopupMenu;
import journeymap.api.v2.client.fullscreen.ThemeButtonDisplay;
import journeymap.api.v2.common.event.ClientEventRegistry;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@JourneyMapPlugin(apiVersion = "2.0.0-SNAPSHOT")
public class MapFrontiersPlugin implements IClientPlugin {
    private static FullscreenMap fullscreenMap;

    @Override
    public void initialize(final IClientAPI jmAPI) {
        MapFrontiersClient.setjmAPI(jmAPI);

        ClientEventRegistry.FULLSCREEN_MAP_CLICK_EVENT.subscribe(MapFrontiers.MODID, (clickEvent) -> {
            if (!Config.fullscreenButtons) {
                return;
            }
            if (fullscreenMap == null) {
                return;
            }

            FullscreenMapEvent.Stage relevantStage;
            if ((fullscreenMap.isEditingVertices() || fullscreenMap.isEditingChunks()) && clickEvent.getButton() == 1) {
                relevantStage = FullscreenMapEvent.Stage.PRE;
            } else {
                relevantStage = FullscreenMapEvent.Stage.POST;
            }
            if (clickEvent.getStage() == relevantStage) {
                boolean cancel = fullscreenMap.mapClicked(clickEvent.dimension, clickEvent.getLocation(), clickEvent.getButton());
                if (cancel) {
                    clickEvent.cancel();
                }
            }
        });

        ClientEventRegistry.FULLSCREEN_MAP_DRAG_EVENT.subscribe(MapFrontiers.MODID, (mouseDraggedEvent) -> {
            if (!Config.fullscreenButtons) {
                return;
            }
            if (fullscreenMap == null) {
                return;
            }

            if (mouseDraggedEvent.getStage() == FullscreenMapEvent.Stage.PRE) {
                boolean cancel = fullscreenMap.mapDragged(mouseDraggedEvent.dimension, mouseDraggedEvent.getLocation());
                if (cancel) {
                    mouseDraggedEvent.cancel();
                }
            }
        });

        ClientEventRegistry.FULLSCREEN_MAP_MOVE_EVENT.subscribe(MapFrontiers.MODID, (mouseMoveEvent) -> {
            if (!Config.fullscreenButtons) {
                return;
            }
            if (fullscreenMap == null) {
                return;
            }

            fullscreenMap.mouseMoved(mouseMoveEvent.dimension, mouseMoveEvent.getLocation());
        });

        ClientEventRegistry.DISPLAY_UPDATE_EVENT.subscribe(MapFrontiers.MODID, (displayUpdateEvent) -> {
            if (!Config.fullscreenButtons) {
                return;
            }

            if (fullscreenMap == null) {
                return;
            }

            if (displayUpdateEvent.uiState.ui == Context.UI.Fullscreen) {
                if (displayUpdateEvent.uiState.active) {
                    fullscreenMap.updateButtons();
                } else {
                    fullscreenMap.stopEditing();
                    fullscreenMap.close();
                    fullscreenMap = null;
                }
            }
        });

        ClientEventRegistry.ADDON_BUTTON_DISPLAY_EVENT.subscribe(MapFrontiers.MODID, (addonButtonDisplayEvent) -> {
            ThemeButtonDisplay buttonDisplay = addonButtonDisplayEvent.getThemeButtonDisplay();
            Screen fullscreen = addonButtonDisplayEvent.getFullscreen().getScreen();
            ClientEventHandler.postAddonButtonDisplayEvent(buttonDisplay, fullscreen);
        });

        ClientEventRegistry.FULLSCREEN_POPUP_MENU_EVENT.subscribe(MapFrontiers.MODID, (fullscreenPopupMenuEvent) -> {
            ModPopupMenu popupMenu = fullscreenPopupMenuEvent.getPopupMenu();
            ClientEventHandler.postFullscreenPopupMenuEvent(popupMenu);
        });

        ClientEventHandler.subscribeAddonButtonDisplayEvent(MapFrontiersPlugin.class, (buttonDisplay, fullscreen) -> {
            if (!Config.fullscreenButtons) {
                return;
            }

            if (fullscreenMap == null) {
                fullscreenMap = new FullscreenMap(jmAPI);
            }

            fullscreenMap.addButtons(buttonDisplay, fullscreen);
        });

        ClientEventHandler.subscribeFullscreenPopupMenuEvent(MapFrontiersPlugin.class, popupMenu -> {
            if (!Config.fullscreenButtons) {
                return;
            }

            if (fullscreenMap != null) {
                fullscreenMap.addPopupMenu(popupMenu);
            }
        });
    }

    @Override
    public String getModId() {
        return MapFrontiers.MODID;
    }

    public static boolean isEditing() {
        if (!Config.fullscreenButtons) {
            return false;
        }

        return fullscreenMap != null && (fullscreenMap.isEditingVertices() || fullscreenMap.isEditingChunks());
    }
}
