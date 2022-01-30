package games.alejandrocoria.mapfrontiers.client.plugin;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.Context;
import journeymap.client.api.display.IThemeButton;
import journeymap.client.api.display.ThemeButtonDisplay;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.event.DisplayUpdateEvent;
import journeymap.client.api.event.FullscreenMapEvent;
import journeymap.client.api.event.forge.FullscreenDisplayEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;

import static java.lang.Math.max;
import static java.lang.Math.pow;

@ParametersAreNonnullByDefault
@journeymap.client.api.ClientPlugin
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MapFrontiers.MODID)
@OnlyIn(Dist.CLIENT)
public class MapFrontiersPlugin implements IClientPlugin {
    private static IClientAPI jmAPI;

    private static FrontierOverlay frontierHighlighted;

    private static IThemeButton buttonNew;
    private static IThemeButton buttonInfo;
    private static IThemeButton buttonEdit;
    private static IThemeButton buttonDelete;

    private static boolean editing = false;
    private static boolean inFullscreenMap = false;

    @Override
    public void initialize(final IClientAPI jmAPI) {
        this.jmAPI = jmAPI;
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
                FullscreenMapEvent.ClickEvent clickEvent = (FullscreenMapEvent.ClickEvent)event;
                mapClicked(clickEvent.dimension, clickEvent.getLocation());
                break;
            case MAP_DRAGGED:
                FullscreenMapEvent.MouseDraggedEvent mouseDraggedEvent = (FullscreenMapEvent.MouseDraggedEvent)event;
                boolean moved = mapDragged(mouseDraggedEvent.dimension, mouseDraggedEvent.getLocation());
                if (moved) {
                    mouseDraggedEvent.cancel();
                }
                break;
            case DISPLAY_UPDATE:
                DisplayUpdateEvent displayEvent = (DisplayUpdateEvent)event;
                if (displayEvent.uiState.ui == Context.UI.Fullscreen) {
                    inFullscreenMap = true;
                    setFrontiersButtonsEnabled(frontierHighlighted != null);
                } else if (inFullscreenMap) {
                    inFullscreenMap = false;
                    if (editing) {
                        stopEditing();
                    }
                    if (frontierHighlighted != null) {
                        frontierHighlighted.setHighlighted(false);
                        frontierHighlighted = null;
                    }
                }
                break;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFullscreenAddonButton(FullscreenDisplayEvent.AddonButtonDisplayEvent event) {
        ThemeButtonDisplay buttonDisplay = event.getThemeButtonDisplay();
        buttonNew = buttonDisplay.addThemeButton("new frontier", "new_frontier", b -> {});
        buttonInfo = buttonDisplay.addThemeButton("frontier info", "info_frontier", b -> {});
        buttonEdit = buttonDisplay.addThemeToggleButton("done editing", "edit frontier", "edit_frontier", editing, b -> buttonEditToggled());
        buttonDelete = buttonDisplay.addThemeButton("delete frontier", "delete_frontier", b -> {});

        setFrontiersButtonsEnabled(false);
    }

    private static void stopEditing() {
        editing = false;
        boolean personalFrontier = frontierHighlighted.getPersonal();
        FrontiersOverlayManager frontierManager = ClientProxy.getFrontiersOverlayManager(personalFrontier);
        frontierManager.clientUpdatefrontier(frontierHighlighted);
    }

    private static void setFrontiersButtonsEnabled(boolean enabled) {
        if (buttonInfo == null) {
            return;
        }

        buttonInfo.setEnabled(enabled);
        buttonEdit.setEnabled(enabled);
        buttonDelete.setEnabled(enabled);
    }

    private static void buttonEditToggled() {
        buttonEdit.toggle();
        boolean toggled = buttonEdit.getToggled();
        if (toggled) {
            editing = true;
        } else {
            stopEditing();
        }
    }

    public static boolean isEditing() {
        return editing;
    }

    public static void mapClicked(ResourceKey<Level> dimension, BlockPos position) {
        double maxDistanceToClosest = pow(2.0, max(4.0 - jmAPI.getUIState(Context.UI.Fullscreen).zoom, 1.0));

        if (editing && frontierHighlighted != null) {
            frontierHighlighted.selectClosestVertex(position, maxDistanceToClosest);
            return;
        }

        FrontiersOverlayManager globalManager = ClientProxy.getFrontiersOverlayManager(false);
        FrontiersOverlayManager personalManager = ClientProxy.getFrontiersOverlayManager(true);

        if (globalManager == null || personalManager == null) {
            return;
        }

        FrontierOverlay newFrontierHighlighted = personalManager.getFrontierInPosition(dimension, position);
        if (newFrontierHighlighted == null) {
            newFrontierHighlighted = globalManager.getFrontierInPosition(dimension, position);
        }

        if (newFrontierHighlighted != null) {
            if (frontierHighlighted == newFrontierHighlighted) {
                frontierHighlighted.selectClosestVertex(position, maxDistanceToClosest);
            } else {
                if (frontierHighlighted != null) {
                    frontierHighlighted.setHighlighted(false);
                }

                frontierHighlighted = newFrontierHighlighted;
                frontierHighlighted.setHighlighted(true);
                setFrontiersButtonsEnabled(true);
            }
        } else if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
            frontierHighlighted = null;
            setFrontiersButtonsEnabled(false);
        }
    }

    public static boolean mapDragged(ResourceKey<Level> dimension, BlockPos position) {
        if (!editing) {
            return false;
        }

        if (frontierHighlighted == null || !frontierHighlighted.getDimension().equals(dimension)) {
            return false;
        }

        if (frontierHighlighted.getSelectedVertexIndex() == -1) {
            return false;
        }

        float snapDistance = (float) pow(2.0, max(4.0 - jmAPI.getUIState(Context.UI.Fullscreen).zoom, 1.0));
        frontierHighlighted.moveSelectedVertex(position, snapDistance);
        return true;
    }
}
