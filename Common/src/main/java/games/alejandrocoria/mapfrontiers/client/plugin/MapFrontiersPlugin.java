package games.alejandrocoria.mapfrontiers.client.plugin;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.FullscreenMap;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.Context;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.event.DisplayUpdateEvent;
import journeymap.client.api.event.FullscreenMapEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;

@ParametersAreNonnullByDefault
@journeymap.client.api.ClientPlugin
public class MapFrontiersPlugin implements IClientPlugin {
    private static FullscreenMap fullscreenMap;

    @Override
    public void initialize(final IClientAPI jmAPI) {
        MapFrontiersClient.setjmAPI(jmAPI);
        jmAPI.subscribe(MapFrontiers.MODID, EnumSet.of(
                ClientEvent.Type.MAP_CLICKED,
                ClientEvent.Type.MAP_DRAGGED,
                ClientEvent.Type.MAP_MOUSE_MOVED,
                ClientEvent.Type.DISPLAY_UPDATE));

        ClientEventHandler.subscribeAddonButtonDisplayEvent(MapFrontiersPlugin.class, buttonDisplay -> {
            if (fullscreenMap == null) {
                fullscreenMap = new FullscreenMap(jmAPI);
            }

            fullscreenMap.addButtons(buttonDisplay);
        });

        ClientEventHandler.subscribeFullscreenPopupMenuEvent(MapFrontiersPlugin.class, popupMenu -> {
            if (fullscreenMap != null) {
                fullscreenMap.addPopupMenu(popupMenu);
            }
        });
    }

    @Override
    public String getModId() {
        return MapFrontiers.MODID;
    }

    @Override
    public void onEvent(ClientEvent event) {
        if (fullscreenMap == null) {
            return;
        }

        switch (event.type) {
            case MAP_CLICKED -> {
                FullscreenMapEvent.ClickEvent clickEvent = (FullscreenMapEvent.ClickEvent) event;
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
            }
            case MAP_DRAGGED -> {
                FullscreenMapEvent.MouseDraggedEvent mouseDraggedEvent = (FullscreenMapEvent.MouseDraggedEvent) event;
                if (mouseDraggedEvent.getStage() == FullscreenMapEvent.Stage.PRE) {
                    boolean cancel = fullscreenMap.mapDragged(mouseDraggedEvent.dimension, mouseDraggedEvent.getLocation());
                    if (cancel) {
                        mouseDraggedEvent.cancel();
                    }
                }
            }
            case MAP_MOUSE_MOVED -> {
                FullscreenMapEvent.MouseMoveEvent mouseMoveEvent = (FullscreenMapEvent.MouseMoveEvent) event;
                fullscreenMap.mouseMoved(mouseMoveEvent.dimension, mouseMoveEvent.getLocation());
            }
            case DISPLAY_UPDATE -> {
                DisplayUpdateEvent displayEvent = (DisplayUpdateEvent) event;
                if (displayEvent.uiState.ui == Context.UI.Fullscreen) {
                    if (displayEvent.uiState.active) {
                        fullscreenMap.updateButtons();
                    } else {
                        fullscreenMap.stopEditing();
                        fullscreenMap.close();
                        fullscreenMap = null;
                    }
                }
            }
        }
    }

    public static boolean isEditing() {
        return fullscreenMap != null && (fullscreenMap.isEditingVertices() || fullscreenMap.isEditingChunks());
    }
}
