package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.Context;
import journeymap.client.api.display.IThemeButton;
import journeymap.client.api.display.ModPopupMenu;
import journeymap.client.api.display.ThemeButtonDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

import static java.lang.Math.max;
import static java.lang.Math.pow;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFullscreenMap {
    private IClientAPI jmAPI;

    private FrontierOverlay frontierHighlighted;

    private IThemeButton buttonNew;
    private IThemeButton buttonInfo;
    private IThemeButton buttonEdit;
    private IThemeButton buttonClosed;
    private IThemeButton buttonDelete;

    private boolean editing = false;

    public GuiFullscreenMap(IClientAPI jmAPI) {
        this.jmAPI = jmAPI;
    }

    public void close() {
        if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
        }
    }

    public void addButtons(ThemeButtonDisplay buttonDisplay) {
        buttonNew = buttonDisplay.addThemeButton("new frontier", "new_frontier", b -> buttonNewPressed());
        buttonInfo = buttonDisplay.addThemeButton("frontier info", "info_frontier", b -> {});
        buttonEdit = buttonDisplay.addThemeToggleButton("done editing", "edit frontier", "edit_frontier", editing, b -> buttonEditToggled());
        buttonClosed = buttonDisplay.addThemeToggleButton("open", "close", "close_frontier", false, b -> buttonClosedToggled());
        buttonDelete = buttonDisplay.addThemeButton("delete frontier", "delete_frontier", b -> buttonDelete());

        updatebuttons();
    }

    public void addPopupMenu(ModPopupMenu popupMenu) {
        if (editing) {
            popupMenu.addMenuItem("add vertex", p -> buttonAddVertex(p));
            if (frontierHighlighted.getSelectedVertexIndex() != -1) {
                popupMenu.addMenuItem("remove vertex", p -> buttonRemoveVertex());
            }
        }
    }

    public void stopEditing() {
        if (editing) {
            editing = false;
            boolean personalFrontier = frontierHighlighted.getPersonal();
            FrontiersOverlayManager frontierManager = ClientProxy.getFrontiersOverlayManager(personalFrontier);
            frontierManager.clientUpdatefrontier(frontierHighlighted);
        }
    }

    public void updatebuttons() {
        if (buttonInfo == null) {
            return;
        }

        buttonNew.setEnabled(!editing);
        buttonInfo.setEnabled(frontierHighlighted != null && !editing);
        buttonEdit.setEnabled(frontierHighlighted != null);
        buttonClosed.setEnabled(frontierHighlighted != null);
        buttonDelete.setEnabled(frontierHighlighted != null && !editing);

        if (frontierHighlighted != null) {
            buttonClosed.setToggled(frontierHighlighted.getClosed());
        } else {
            buttonClosed.setToggled(false);
        }
    }

    private void buttonNewPressed() {
        if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
        }

        // @Incomplete
        ClientProxy.getFrontiersOverlayManager(false).clientCreateNewfrontier(jmAPI.getUIState(Context.UI.Fullscreen).dimension);

        updatebuttons();
    }

    private void buttonEditToggled() {
        buttonEdit.toggle();
        boolean toggled = buttonEdit.getToggled();
        if (toggled) {
            editing = true;
        } else {
            stopEditing();
        }

        updatebuttons();
    }

    private void buttonClosedToggled() {
        frontierHighlighted.setClosed(!buttonClosed.getToggled());

        updatebuttons();
    }

    private void buttonDelete() {
        if (editing) {
            stopEditing();
        }

        boolean personalFrontier = frontierHighlighted.getPersonal();
        FrontiersOverlayManager frontierManager = ClientProxy.getFrontiersOverlayManager(personalFrontier);
        frontierManager.clientDeleteFrontier(frontierHighlighted);
        frontierHighlighted = null;
        updatebuttons();
    }

    private void buttonAddVertex(BlockPos pos) {
        frontierHighlighted.selectClosestEdge(pos);
        frontierHighlighted.addVertex(pos);

        updatebuttons();
    }

    private void buttonRemoveVertex() {
        frontierHighlighted.removeSelectedVertex();

        updatebuttons();
    }

    public void newFrontierMessage(FrontierOverlay frontierOverlay, int playerID) {
        if (frontierOverlay.getDimension() != jmAPI.getUIState(Context.UI.Fullscreen).dimension) {
            return;
        }

        if (playerID == -1 || Minecraft.getInstance().player.getId() == playerID) {
            stopEditing();
            if (frontierHighlighted != null) {
                frontierHighlighted.setHighlighted(false);
            }

            frontierHighlighted = frontierOverlay;
            frontierHighlighted.setHighlighted(true);

            updatebuttons();
        }
    }

    public boolean isEditing() {
        return editing;
    }

    public void mapClicked(ResourceKey<Level> dimension, BlockPos position) {
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

        FrontierOverlay newFrontierHighlighted = personalManager.getFrontierInPosition(dimension, position, maxDistanceToClosest);
        if (newFrontierHighlighted == null) {
            newFrontierHighlighted = globalManager.getFrontierInPosition(dimension, position, maxDistanceToClosest);
        }

        if (newFrontierHighlighted != null) {
            if (frontierHighlighted != newFrontierHighlighted) {
                if (frontierHighlighted != null) {
                    frontierHighlighted.setHighlighted(false);
                }

                frontierHighlighted = newFrontierHighlighted;
                frontierHighlighted.setHighlighted(true);
            }
        } else if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
            frontierHighlighted = null;
        }

        updatebuttons();
    }

    public boolean mapDragged(ResourceKey<Level> dimension, BlockPos position) {
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
