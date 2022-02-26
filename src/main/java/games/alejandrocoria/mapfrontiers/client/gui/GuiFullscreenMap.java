package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.event.DeletedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.NewFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
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
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.ParametersAreNonnullByDefault;

import static java.lang.Math.max;
import static java.lang.Math.pow;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFullscreenMap {
    private IClientAPI jmAPI;

    private FrontierOverlay frontierHighlighted;

    private IThemeButton buttonFrontiers;
    private IThemeButton buttonNew;
    private IThemeButton buttonInfo;
    private IThemeButton buttonEdit;
    private IThemeButton buttonClosed;
    private IThemeButton buttonDelete;

    private boolean editing = false;

    public GuiFullscreenMap(IClientAPI jmAPI) {
        this.jmAPI = jmAPI;
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void close() {
        if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
        }
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    public void addButtons(ThemeButtonDisplay buttonDisplay) {
        buttonFrontiers = buttonDisplay.addThemeButton("frontiers...", "frontiers", b -> buttonFrontiersPressed());
        buttonNew = buttonDisplay.addThemeButton("new frontier", "new_frontier", b -> buttonNewPressed());
        buttonInfo = buttonDisplay.addThemeButton("frontier info", "info_frontier", b -> buttonInfoPressed());
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatedSettingsProfileEvent(UpdatedSettingsProfileEvent event) {
        updatebuttons();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onNewFrontierEvent(NewFrontierEvent event) {
        if (event.frontierOverlay.getDimension() != jmAPI.getUIState(Context.UI.Fullscreen).dimension) {
            return;
        }

        if (event.playerID == -1 || Minecraft.getInstance().player.getId() == event.playerID) {
            stopEditing();
            if (frontierHighlighted != null) {
                frontierHighlighted.setHighlighted(false);
            }

            frontierHighlighted = event.frontierOverlay;
            frontierHighlighted.setHighlighted(true);

            updatebuttons();

            if (ConfigData.afterCreatingFrontier == ConfigData.AfterCreatingFrontier.Edit) {
                buttonEditToggled();
            } else if (ConfigData.afterCreatingFrontier == ConfigData.AfterCreatingFrontier.Info) {
                buttonInfoPressed();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatedFrontierEvent(UpdatedFrontierEvent event) {
        if (frontierHighlighted != null && frontierHighlighted.getId().equals(event.frontierOverlay.getId())) {
            frontierHighlighted = event.frontierOverlay;
            frontierHighlighted.setHighlighted(true);
            editing = false;
            updatebuttons();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeletedFrontierEvent(DeletedFrontierEvent event) {
        if (frontierHighlighted != null && frontierHighlighted.getId().equals(event.frontierID)) {
            frontierHighlighted = null;
            editing = false;
            updatebuttons();
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

        SettingsProfile profile = ClientProxy.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(Minecraft.getInstance().player);
        SettingsProfile.AvailableActions actions = profile.getAvailableActions(frontierHighlighted, playerUser);

        buttonFrontiers.setEnabled(!editing);
        buttonNew.setEnabled(actions.canCreate && !editing);
        buttonInfo.setEnabled(frontierHighlighted != null && !editing);
        buttonEdit.setEnabled(actions.canUpdate);
        buttonClosed.setEnabled(actions.canUpdate);
        buttonDelete.setEnabled(actions.canDelete && !editing);

        if (frontierHighlighted != null) {
            buttonClosed.setToggled(frontierHighlighted.getClosed());
        } else {
            buttonClosed.setToggled(false);
        }
    }

    private void buttonFrontiersPressed() {
        ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new GuiFrontierList(jmAPI));
    }

    private void buttonNewPressed() {
        if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
        }

        ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new GuiNewFrontier(jmAPI));

        updatebuttons();
    }

    private void buttonInfoPressed() {
        ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new GuiFrontierInfo(jmAPI, frontierHighlighted));
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
