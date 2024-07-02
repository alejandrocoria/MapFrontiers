package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.screen.FrontierInfo;
import games.alejandrocoria.mapfrontiers.client.gui.screen.FrontierList;
import games.alejandrocoria.mapfrontiers.client.gui.screen.NewFrontier;
import games.alejandrocoria.mapfrontiers.client.gui.screen.StackeableScreen;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.fullscreen.IThemeButton;
import journeymap.api.v2.client.fullscreen.ModPopupMenu;
import journeymap.api.v2.client.fullscreen.ThemeButtonDisplay;
import journeymap.api.v2.client.util.UIState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class FullscreenMap {
    private enum ChunkDrawing {
        Nothing, Adding, Removing
    }

    private final IClientAPI jmAPI;
    private Screen fullscreen;

    private FrontierOverlay frontierHighlighted;

    private IThemeButton buttonFrontiers;
    private IThemeButton buttonNew;
    private IThemeButton buttonInfo;
    private IThemeButton buttonEdit;
    private IThemeButton buttonVisible;
    private IThemeButton buttonDelete;

    private boolean editing = false;
    private ChunkDrawing drawingChunk = ChunkDrawing.Nothing;
    private ChunkPos lastEditedChunk;

    public FullscreenMap(IClientAPI jmAPI) {
        this.jmAPI = jmAPI;

        ClientEventHandler.subscribeDeletedFrontierEvent(this, frontierID -> {
            if (frontierHighlighted != null && frontierHighlighted.getId().equals(frontierID)) {
                frontierHighlighted = null;
                editing = false;
                updateButtons();
            }
        });

        ClientEventHandler.subscribeNewFrontierEvent(this, (frontierOverlay, playerID) -> {
            UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
            if (uiState == null || frontierOverlay.getDimension() != uiState.dimension) {
                return;
            }

            Player localPlayer = Minecraft.getInstance().player;
            if (playerID == -1 || (localPlayer != null && localPlayer.getId() == playerID)) {
                stopEditing();
                if (frontierHighlighted != null) {
                    frontierHighlighted.setHighlighted(false);
                }

                frontierHighlighted = frontierOverlay;
                frontierHighlighted.setHighlighted(true);

                updateButtons();

                if (Config.afterCreatingFrontier == Config.AfterCreatingFrontier.Edit) {
                    buttonEditToggled();
                } else if (Config.afterCreatingFrontier == Config.AfterCreatingFrontier.Info) {
                    buttonInfoPressed();
                }
            }
        });

        ClientEventHandler.subscribeUpdatedFrontierEvent(this, (frontierOverlay, playerID) -> {
            if (frontierHighlighted != null && frontierHighlighted.getId().equals(frontierOverlay.getId())) {
                frontierHighlighted = frontierOverlay;
                frontierHighlighted.setHighlighted(true);
                editing = false;
                updateButtons();
            }
        });

        ClientEventHandler.subscribeUpdatedSettingsProfileEvent(this, profile -> {
            updateButtons();
        });

        ClientEventHandler.subscribeMouseReleaseEvent(this, button -> {
            if (button != 1) {
                return;
            }

            if (!editing || drawingChunk == ChunkDrawing.Nothing || frontierHighlighted.getMode() != FrontierData.Mode.Chunk) {
                return;
            }

            drawingChunk = ChunkDrawing.Nothing;
        });
    }

    public void close() {
        if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
        }
        ClientEventHandler.unsuscribeAllEvents(this);
    }

    public void addButtons(ThemeButtonDisplay buttonDisplay, Screen fullscreen) {
        this.fullscreen = fullscreen;

        String path = "textures/gui/journeymap/";
        buttonFrontiers = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_frontiers"), ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, path + "frontiers.png"), b -> buttonFrontiersPressed());
        buttonNew = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_new_frontier"), ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, path + "new_frontier.png"), b -> buttonNewPressed());
        buttonInfo = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_frontier_info"), ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, path + "info_frontier.png"), b -> buttonInfoPressed());
        buttonEdit = buttonDisplay.addThemeToggleButton(I18n.get("mapfrontiers.button_done_editing"), I18n.get("mapfrontiers.button_edit_frontier"),
                ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, path + "edit_frontier.png"), editing, b -> buttonEditToggled());
        buttonVisible = buttonDisplay.addThemeToggleButton(I18n.get("mapfrontiers.button_hide_frontier"), I18n.get("mapfrontiers.button_show_frontier"),
                ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, path + "visible_frontier.png"), false, b -> buttonVisibleToggled());
        buttonDelete = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_delete_frontier"), ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, path + "delete_frontier.png"), b -> buttonDelete());

        updateButtons();
    }

    public void addPopupMenu(ModPopupMenu popupMenu) {
        if (editing && frontierHighlighted.getMode() == FrontierData.Mode.Vertex) {
            popupMenu.addMenuItem(I18n.get("mapfrontiers.add_vertex"), this::buttonAddVertex);
            if (frontierHighlighted.getSelectedVertexIndex() != -1) {
                popupMenu.addMenuItem(I18n.get("mapfrontiers.remove_vertex"), p -> buttonRemoveVertex());
            }
        }
    }

    public void stopEditing() {
        if (editing) {
            editing = false;
            boolean personalFrontier = frontierHighlighted.getPersonal();
            FrontiersOverlayManager frontierManager = MapFrontiersClient.getFrontiersOverlayManager(personalFrontier);
            frontierManager.clientUpdatefrontier(frontierHighlighted);
        }
    }

    public void updateButtons() {
        Player player = Minecraft.getInstance().player;

        if (buttonInfo == null || player == null) {
            return;
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(player);
        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontierHighlighted, playerUser);

        buttonFrontiers.setEnabled(!editing);
        buttonNew.setEnabled(!editing);
        buttonInfo.setEnabled(frontierHighlighted != null && !editing);
        buttonEdit.setEnabled(actions.canUpdate && frontierHighlighted.getVisible() && frontierHighlighted.getFullscreenVisible());
        buttonVisible.setEnabled(actions.canUpdate && !editing);
        buttonDelete.setEnabled(actions.canDelete && !editing);

        if (frontierHighlighted != null) {
            buttonVisible.setToggled(frontierHighlighted.getVisible() && frontierHighlighted.getFullscreenVisible());
        } else {
            buttonVisible.setToggled(false);
        }
    }

    private void buttonFrontiersPressed() {
        StackeableScreen.open(new FrontierList(jmAPI, this, fullscreen));
    }

    private void buttonNewPressed() {
        if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
            frontierHighlighted = null;
        }

        StackeableScreen.open(new NewFrontier(jmAPI, fullscreen));

        updateButtons();
    }

    private void buttonInfoPressed() {
        StackeableScreen.open(new FrontierInfo(jmAPI, frontierHighlighted, fullscreen));
    }

    private void buttonEditToggled() {
        buttonEdit.toggle();
        boolean toggled = buttonEdit.getToggled();
        if (toggled) {
            editing = true;
            drawingChunk = ChunkDrawing.Nothing;
        } else {
            stopEditing();
        }

        updateButtons();
    }

    private void buttonVisibleToggled() {
        frontierHighlighted.setVisible(!buttonVisible.getToggled());

        boolean personalFrontier = frontierHighlighted.getPersonal();
        FrontiersOverlayManager frontierManager = MapFrontiersClient.getFrontiersOverlayManager(personalFrontier);
        frontierManager.clientUpdatefrontier(frontierHighlighted);

        updateButtons();
    }

    private void buttonDelete() {
        if (editing) {
            stopEditing();
        }

        boolean personalFrontier = frontierHighlighted.getPersonal();
        FrontiersOverlayManager frontierManager = MapFrontiersClient.getFrontiersOverlayManager(personalFrontier);
        frontierManager.clientDeleteFrontier(frontierHighlighted);
        frontierHighlighted = null;
        updateButtons();
    }

    private void buttonAddVertex(BlockPos pos) {
        frontierHighlighted.selectClosestEdge(pos);
        frontierHighlighted.addVertex(pos);

        updateButtons();
    }

    private void buttonRemoveVertex() {
        frontierHighlighted.removeSelectedVertex();

        updateButtons();
    }

    public boolean isEditingVertices() {
        return editing && frontierHighlighted.getMode() == FrontierData.Mode.Vertex;
    }

    public boolean isEditingChunks() {
        return editing && frontierHighlighted.getMode() == FrontierData.Mode.Chunk;
    }

    public FrontierOverlay getSelected() {
        return frontierHighlighted;
    }

    public void selectFrontier(@Nullable FrontierOverlay frontier) {
        UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
        if (uiState != null && frontier != null && frontier.getDimension().equals(uiState.dimension)) {
            if (frontierHighlighted != frontier) {
                if (frontierHighlighted != null) {
                    frontierHighlighted.setHighlighted(false);
                }

                frontierHighlighted = frontier;
                frontierHighlighted.setHighlighted(true);
            }
        } else if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
            frontierHighlighted = null;
        }

        updateButtons();
    }

    public boolean mapClicked(ResourceKey<Level> dimension, BlockPos position, int button) {
        UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
        if (uiState == null) {
            return false;
        }

        double maxDistanceToClosest = Math.max(2.0, 8192.0 / uiState.zoom);

        if (editing && frontierHighlighted != null) {
            if (frontierHighlighted.getMode() == FrontierData.Mode.Vertex) {
                frontierHighlighted.selectClosestVertex(position, maxDistanceToClosest);
            } else if (button == 1) {
                lastEditedChunk = new ChunkPos(position);
                if (frontierHighlighted.toggleChunk(lastEditedChunk)) {
                    drawingChunk = ChunkDrawing.Adding;
                } else {
                    drawingChunk = ChunkDrawing.Removing;
                }
                return true;
            }
            return false;
        }

        FrontiersOverlayManager globalManager = MapFrontiersClient.getFrontiersOverlayManager(false);
        FrontiersOverlayManager personalManager = MapFrontiersClient.getFrontiersOverlayManager(true);

        if (globalManager == null || personalManager == null) {
            return false;
        }

        FrontierOverlay newFrontierHighlighted = personalManager.getFrontierInPosition(dimension, position, maxDistanceToClosest);
        if (newFrontierHighlighted == null) {
            newFrontierHighlighted = globalManager.getFrontierInPosition(dimension, position, maxDistanceToClosest);
        }

        selectFrontier(newFrontierHighlighted);

        return false;
    }

    public boolean mapDragged(ResourceKey<Level> dimension, BlockPos position) {
        UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);

        if (uiState == null || !editing) {
            return false;
        }

        if (frontierHighlighted == null || !frontierHighlighted.getDimension().equals(dimension)) {
            return false;
        }

        if (frontierHighlighted.getMode() == FrontierData.Mode.Chunk) {
            return false;
        }

        if (frontierHighlighted.getSelectedVertexIndex() == -1) {
            return false;
        }

        float snapDistance = (float) Math.pow(2.0, Math.max(4.0 - uiState.zoom, 1.0));
        frontierHighlighted.moveSelectedVertex(position, snapDistance);
        return true;
    }

    public void mouseMoved(ResourceKey<Level> dimension, BlockPos position) {
        if (!editing || drawingChunk == ChunkDrawing.Nothing) {
            return;
        }

        if (frontierHighlighted == null || !frontierHighlighted.getDimension().equals(dimension)) {
            return;
        }

        if (frontierHighlighted.getMode() != FrontierData.Mode.Chunk) {
            return;
        }

        ChunkPos chunk = new ChunkPos(position);
        if (chunk.equals(lastEditedChunk)) {
            return;
        }

        lastEditedChunk = chunk;

        if (drawingChunk == ChunkDrawing.Adding) {
            frontierHighlighted.addChunk(chunk);
        } else {
            frontierHighlighted.removeChunk(chunk);
        }
    }
}
