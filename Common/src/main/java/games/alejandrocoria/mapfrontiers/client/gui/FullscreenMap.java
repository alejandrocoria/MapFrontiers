package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.ConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.DeleteConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.FrontierInfo;
import games.alejandrocoria.mapfrontiers.client.gui.screen.FrontierList;
import games.alejandrocoria.mapfrontiers.client.gui.screen.NewFrontier;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.fullscreen.IThemeButton;
import journeymap.api.v2.client.fullscreen.ModPopupMenu;
import journeymap.api.v2.client.fullscreen.ThemeButtonDisplay;
import journeymap.api.v2.client.util.UIState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class FullscreenMap {
    private enum ChunkDrawing {
        Nothing, Adding, Removing
    }

    private final IClientAPI jmAPI;

    private FrontierOverlay frontierHighlighted;

    private IThemeButton buttonFrontiers;
    private IThemeButton buttonNew;
    private IThemeButton buttonInfo;
    private IThemeButton buttonEdit;
    private IThemeButton buttonVisible;
    private IThemeButton buttonDelete;

    private boolean editing = false;
    private boolean shapeDirty = false;
    private boolean relocating = false;
    private BlockPos relocatingPrevPos;
    private ChunkDrawing drawingChunk = ChunkDrawing.Nothing;
    private ChunkPos lastEditedChunk;

    public FullscreenMap(IClientAPI jmAPI) {
        this.jmAPI = jmAPI;

        MapFrontiersClient.getFrontierEvents().subscribeDeleted(this, frontierID -> {
            if (frontierHighlighted != null && frontierHighlighted.getId().equals(frontierID)) {
                frontierHighlighted = null;
                editing = false;
                shapeDirty = false;
                relocating = false;
                updateButtons();
            }
        });

        MapFrontiersClient.getFrontierEvents().subscribeCreated(this, (frontierOverlay, playerID) -> {
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

                if (ClientConfig.AFTER_CREATING_FRONTIER.get() == ClientConfig.AfterCreatingFrontier.EditShape) {
                    buttonEditToggled();
                } else if (ClientConfig.AFTER_CREATING_FRONTIER.get() == ClientConfig.AfterCreatingFrontier.InfoScreen) {
                    buttonInfoPressed();
                }
            }
        });

        MapFrontiersClient.getFrontierEvents().subscribeUpdated(this, (frontierOverlay, playerID) -> {
            if (frontierHighlighted != null && frontierHighlighted.getId().equals(frontierOverlay.getId())) {
                frontierHighlighted = frontierOverlay;
                frontierHighlighted.setHighlighted(true);
                editing = false;
                shapeDirty = false;
                relocating = false;
                updateButtons();
            }
        });

        MapFrontiersClient.getSettingsProfileEvents().subscribeUpdated(this, profile -> {
            updateButtons();
        });

        ClientGlobalEvents.subscribeUpdatedConfigEvent(this, this::updateButtons);

        ClientGlobalEvents.subscribeMouseReleaseEvent(this, button -> {
            if (button != 1) {
                return;
            }

            relocating = false;

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
        MapFrontiersClient.getFrontierEvents().unsubscribe(this);
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        ClientGlobalEvents.unsubscribeAllEvents(this);
    }

    public void addButtons(ThemeButtonDisplay buttonDisplay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        String path = "textures/gui/journeymap/";
        buttonFrontiers = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_frontiers"), Identifier.fromNamespaceAndPath(MapFrontiers.MODID, path + "frontiers.png"), b -> buttonFrontiersPressed());
        buttonNew = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_new_frontier"), Identifier.fromNamespaceAndPath(MapFrontiers.MODID, path + "new_frontier.png"), b -> buttonNewPressed(mc.player.blockPosition()));
        buttonInfo = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_frontier_info"), Identifier.fromNamespaceAndPath(MapFrontiers.MODID, path + "info_frontier.png"), b -> buttonInfoPressed());
        buttonEdit = buttonDisplay.addThemeToggleButton(I18n.get("mapfrontiers.button_done_editing"), I18n.get("mapfrontiers.button_edit_frontier"),
                Identifier.fromNamespaceAndPath(MapFrontiers.MODID, path + "edit_frontier.png"), editing, b -> buttonEditToggled());
        buttonVisible = buttonDisplay.addThemeToggleButton(I18n.get("mapfrontiers.button_hide_frontier"), I18n.get("mapfrontiers.button_show_frontier"),
                Identifier.fromNamespaceAndPath(MapFrontiers.MODID, path + "visible_frontier.png"), false, b -> buttonVisibleToggled());
        buttonDelete = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_delete_frontier"), Identifier.fromNamespaceAndPath(MapFrontiers.MODID, path + "delete_frontier.png"), b -> buttonDelete());

        updateButtons();
    }

    public void addPopupMenu(ModPopupMenu popupMenu) {
        if (editing) {
            if (frontierHighlighted.getMode() == FrontierData.Mode.Vertex) {
                popupMenu.addMenuItem(I18n.get("mapfrontiers.add_vertex"), this::buttonAddVertex);
                if (frontierHighlighted.getSelectedVertexIndex() != -1) {
                    popupMenu.addMenuItem(I18n.get("mapfrontiers.remove_vertex"), p -> buttonRemoveVertex());
                }
            } else if (frontierHighlighted.getMode() == FrontierData.Mode.Path) {
                popupMenu.addMenuItem(I18n.get("mapfrontiers.add_point"), this::buttonAddPoint);
                popupMenu.addMenuItem(I18n.get("mapfrontiers.add_point_before_start"), this::buttonAddPointBeforeStart);
                popupMenu.addMenuItem(I18n.get("mapfrontiers.add_point_after_end"), this::buttonAddPointAfterEnd);
                if (frontierHighlighted.getSelectedPointIndex() != -1) {
                    popupMenu.addMenuItem(I18n.get("mapfrontiers.remove_point"), p -> buttonRemovePoint());
                }
                if (frontierHighlighted.getPointCount() > 1) {
                    popupMenu.addMenuItem(I18n.get("mapfrontiers.invert_direction"), p -> buttonInvertPathDirection());
                }
            } else {
                if (frontierHighlighted.hasChunk(lastEditedChunk)) {
                    List<ChunkPos> chunksToRemove = frontierHighlighted.getConnectedChunks(lastEditedChunk);
                    if (!chunksToRemove.isEmpty()) {
                        popupMenu.addMenuItem(I18n.get("mapfrontiers.remove_connected_chunks"), p -> buttonRemoveConnected(chunksToRemove));
                    }
                } else {
                    List<ChunkPos> chunksToFill = frontierHighlighted.getClosedRegion(lastEditedChunk);
                    if (!chunksToFill.isEmpty()) {
                        popupMenu.addMenuItem(I18n.get("mapfrontiers.fill_region_of_chunks"), p -> buttonFillRegion(chunksToFill));
                    }
                }
            }
            popupMenu.addMenuItem(I18n.get("mapfrontiers.button_done_editing"), p -> buttonEditToggled());
        } else {
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
            SettingsUser playerUser = new SettingsUser(player);
            SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontierHighlighted, playerUser);

            ModPopupMenu subMenu = popupMenu.createSubItemList("MapFrontiers");
            subMenu.addMenuItem(I18n.get("mapfrontiers.button_frontiers"), p -> buttonFrontiersPressed());
            subMenu.addMenuItem(I18n.get("mapfrontiers.button_new_frontier"), p -> buttonNewPressed(p));
            if (frontierHighlighted != null) {
                subMenu.addMenuItem(I18n.get("mapfrontiers.button_frontier_info"), p -> buttonInfoPressed());
            }
            if (actions.canUpdate && frontierHighlighted.getVisibility(FrontierData.VisibilityData.Visibility.Frontier)
                    && frontierHighlighted.getVisibility(FrontierData.VisibilityData.Visibility.Fullscreen)) {
                subMenu.addMenuItem(I18n.get("mapfrontiers.button_edit_frontier"), p -> buttonEditToggled());
            }
            if (actions.canUpdate) {
                subMenu.addMenuItem(I18n.get("mapfrontiers.button_hide_frontier"), p -> buttonVisibleToggled());
            }
            if (actions.canUpdate) {
                subMenu.addMenuItem(I18n.get("mapfrontiers.button_delete_frontier"), p -> buttonDelete());
            }
        }
    }

    public void stopEditing() {
        if (editing) {
            editing = false;
            relocating = false;
            frontierHighlighted.clearSelectedEditablePoint();
            if (shapeDirty) {
                FrontierChange change = new FrontierChange();
                change.setShape(frontierHighlighted.getVertices(), frontierHighlighted.getChunks(), frontierHighlighted.getPoints(),
                        frontierHighlighted.getMode());
                MapFrontiersClient.getOperationService().updateFrontier(frontierHighlighted, change);
                shapeDirty = false;
            }
        }
    }

    public void updateButtons() {
        Player player = Minecraft.getInstance().player;

        if (buttonInfo == null || player == null) {
            return;
        }

        buttonFrontiers.setDrawButton(ClientConfig.FULLSCREEN_BUTTONS.get());
        buttonNew.setDrawButton(ClientConfig.FULLSCREEN_BUTTONS.get());
        buttonInfo.setDrawButton(ClientConfig.FULLSCREEN_BUTTONS.get());
        buttonEdit.setDrawButton(ClientConfig.FULLSCREEN_BUTTONS.get());
        buttonVisible.setDrawButton(ClientConfig.FULLSCREEN_BUTTONS.get());
        buttonDelete.setDrawButton(ClientConfig.FULLSCREEN_BUTTONS.get());

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(player);
        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontierHighlighted, playerUser);
        UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
        boolean selectedFrontierVisible = frontierHighlighted != null
                && uiState != null
                && frontierHighlighted.getDimension().equals(uiState.dimension)
                && frontierHighlighted.isVisibleOnFullscreenMap(uiState.mapType);

        buttonFrontiers.setEnabled(!editing);
        buttonNew.setEnabled(!editing);
        buttonInfo.setEnabled(frontierHighlighted != null && !editing);
        buttonEdit.setEnabled(actions.canUpdate && selectedFrontierVisible);
        buttonVisible.setEnabled(actions.canUpdate && !editing);
        buttonDelete.setEnabled(actions.canDelete && !editing);

        if (frontierHighlighted != null) {
            buttonVisible.setToggled(frontierHighlighted.getVisibility(FrontierData.VisibilityData.Visibility.Frontier) && frontierHighlighted.getVisibility(FrontierData.VisibilityData.Visibility.Fullscreen));
        } else {
            buttonVisible.setToggled(false);
        }
    }

    private void buttonFrontiersPressed() {
        new FrontierList(jmAPI, this).display();
    }

    private void buttonNewPressed(BlockPos centerPos) {
        if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
            frontierHighlighted = null;
        }

        new NewFrontier(jmAPI, centerPos).display();

        updateButtons();
    }

    private void buttonInfoPressed() {
        new FrontierInfo(jmAPI, frontierHighlighted).display();
    }

    private void buttonEditToggled() {
        buttonEdit.toggle();
        if (!editing) {
            editing = true;
            shapeDirty = false;
            drawingChunk = ChunkDrawing.Nothing;
            frontierHighlighted.clearSelectedEditablePoint();
        } else {
            stopEditing();
        }

        updateButtons();
    }

    private void buttonVisibleToggled() {
        frontierHighlighted.setVisibility(FrontierData.VisibilityData.Visibility.Frontier, !frontierHighlighted.getVisibility(FrontierData.VisibilityData.Visibility.Frontier));
        MapFrontiersClient.getOperationService().updateFrontier(frontierHighlighted);

        updateButtons();
    }

    private void buttonDelete() {
        if (ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.get()) {
            new DeleteConfirmationDialog(
                    "mapfrontiers.delete_frontier_dialog",
                    response -> {
                        if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                            ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.set(false);
                            ClientGlobalEvents.postUpdatedConfigEvent();
                        }
                        deleteFrontier();
                    }
            ).display();
        } else {
            deleteFrontier();
        }
    }

    private void deleteFrontier() {
        if (editing) {
            stopEditing();
        }
        MapFrontiersClient.getOperationService().deleteFrontier(frontierHighlighted);
        frontierHighlighted = null;
        updateButtons();
    }

    private void buttonAddVertex(BlockPos pos) {
        frontierHighlighted.selectClosestEdge(pos);
        frontierHighlighted.addVertex(pos);
        shapeDirty = true;

        updateButtons();
    }

    private void buttonRemoveVertex() {
        int vertexCount = frontierHighlighted.getVertexCount();
        frontierHighlighted.removeSelectedVertex();
        if (frontierHighlighted.getVertexCount() != vertexCount) {
            shapeDirty = true;
        }

        updateButtons();
    }

    private void buttonAddPoint(BlockPos pos) {
        int pointCount = frontierHighlighted.getPointCount();
        frontierHighlighted.insertPathPoint(pos);
        if (frontierHighlighted.getPointCount() != pointCount) {
            shapeDirty = true;
        }

        updateButtons();
    }

    private void buttonAddPointBeforeStart(BlockPos pos) {
        int pointCount = frontierHighlighted.getPointCount();
        frontierHighlighted.addPathPointBeforeStart(pos);
        if (frontierHighlighted.getPointCount() != pointCount) {
            shapeDirty = true;
        }

        updateButtons();
    }

    private void buttonAddPointAfterEnd(BlockPos pos) {
        int pointCount = frontierHighlighted.getPointCount();
        frontierHighlighted.addPathPointAfterEnd(pos);
        if (frontierHighlighted.getPointCount() != pointCount) {
            shapeDirty = true;
        }

        updateButtons();
    }

    private void buttonRemovePoint() {
        int pointCount = frontierHighlighted.getPointCount();
        frontierHighlighted.removeSelectedPoint();
        if (frontierHighlighted.getPointCount() != pointCount) {
            shapeDirty = true;
        }

        updateButtons();
    }

    private void buttonInvertPathDirection() {
        frontierHighlighted.invertPathDirection();
        shapeDirty = true;
        updateButtons();
    }

    private void buttonRemoveConnected(List<ChunkPos> chunks) {
        for (ChunkPos chunk : chunks) {
            frontierHighlighted.removeChunk(chunk);
        }
        if (!chunks.isEmpty()) {
            shapeDirty = true;
        }
    }

    private void buttonFillRegion(List<ChunkPos> chunks) {
        for (ChunkPos chunk : chunks) {
            frontierHighlighted.addChunk(chunk);
        }
        if (!chunks.isEmpty()) {
            shapeDirty = true;
        }
    }

    public boolean isEditingVertices() {
        return editing && frontierHighlighted.getMode() == FrontierData.Mode.Vertex;
    }

    public boolean isEditingPaths() {
        return editing && frontierHighlighted.getMode() == FrontierData.Mode.Path;
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
            if (ScreenHelper.hasControlDown() && button == 1) {
                relocating = true;
                relocatingPrevPos = position;
                return true;
            }
            else if (frontierHighlighted.getMode() == FrontierData.Mode.Vertex) {
                frontierHighlighted.selectClosestVertex(position, maxDistanceToClosest);
            } else if (frontierHighlighted.getMode() == FrontierData.Mode.Path) {
                frontierHighlighted.selectClosestPoint(position, maxDistanceToClosest);
            } else if (button == 1) {
                lastEditedChunk = ChunkPos.containing(position);
                if (ScreenHelper.hasShiftDown()) {
                    return false;
                }else {
                    if (frontierHighlighted.toggleChunk(lastEditedChunk)) {
                        drawingChunk = ChunkDrawing.Adding;
                    } else {
                        drawingChunk = ChunkDrawing.Removing;
                    }
                    shapeDirty = true;
                }
                return true;
            }
            return false;
        }

        if (ClientConfig.FRONTIER_VISIBILITY.get() == ClientConfig.Visibility.Never) {
            selectFrontier(null);
            return false;
        }

        List<FrontierOverlay> frontiers = MapFrontiersClient.getFrontiersInPosition(dimension, position, maxDistanceToClosest, uiState.mapType);
        if (frontiers.isEmpty()) {
            selectFrontier(null);
        } else if (frontiers.size() == 1 || frontierHighlighted == null) {
            selectFrontier(frontiers.getFirst());
        } else {
            int i = frontiers.indexOf(frontierHighlighted);
            i = (i + 1) % frontiers.size();
            selectFrontier(frontiers.get(i));
        }

        return false;
    }

    public boolean mapDragged(ResourceKey<Level> dimension, BlockPos position) {
        UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);

        if (uiState == null || !editing || relocating) {
            return false;
        }

        if (frontierHighlighted == null || !frontierHighlighted.getDimension().equals(dimension)) {
            return false;
        }

        if (frontierHighlighted.getMode() == FrontierData.Mode.Chunk) {
            return false;
        }

        if (frontierHighlighted.getSelectedEditablePointIndex() == -1) {
            return false;
        }

        float snapDistance = 512.f / uiState.zoom * ClientConfig.SNAP_DISTANCE.get();
        frontierHighlighted.moveSelectedEditablePoint(position, snapDistance);
        shapeDirty = true;
        return true;
    }

    public void mouseMoved(ResourceKey<Level> dimension, BlockPos position) {
        if (!editing) {
            return;
        }

        if (relocating) {
            if (frontierHighlighted.getMode() == FrontierData.Mode.Vertex) {
                if (!position.equals(relocatingPrevPos)) {
                    frontierHighlighted.moveAllVertices(position.subtract(relocatingPrevPos));
                    relocatingPrevPos = position;
                    shapeDirty = true;
                }
            } else if (frontierHighlighted.getMode() == FrontierData.Mode.Path) {
                if (!position.equals(relocatingPrevPos)) {
                    frontierHighlighted.moveAllPathPoints(position.subtract(relocatingPrevPos));
                    relocatingPrevPos = position;
                    shapeDirty = true;
                }
            } else {
                ChunkPos chunkPos = ChunkPos.containing(position);
                ChunkPos prevChunkPos = ChunkPos.containing(relocatingPrevPos);
                if (!chunkPos.equals(prevChunkPos)) {
                    frontierHighlighted.moveAllChunks(new ChunkPos(chunkPos.x() - prevChunkPos.x(), chunkPos.z() - prevChunkPos.z()));
                    relocatingPrevPos = position;
                    shapeDirty = true;
                }
            }
            return;
        }

        if (drawingChunk == ChunkDrawing.Nothing) {
            return;
        }

        if (frontierHighlighted == null || !frontierHighlighted.getDimension().equals(dimension)) {
            return;
        }

        if (frontierHighlighted.getMode() != FrontierData.Mode.Chunk) {
            return;
        }

        ChunkPos chunk = ChunkPos.containing(position);
        if (chunk.equals(lastEditedChunk)) {
            return;
        }

        lastEditedChunk = chunk;

        if (drawingChunk == ChunkDrawing.Adding) {
            frontierHighlighted.addChunk(chunk);
        } else {
            frontierHighlighted.removeChunk(chunk);
        }
        shapeDirty = true;
    }
}
