package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.AfterCreatingFrontier;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.FrontierDisplayVisibility;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.DeleteCollectionConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.DeleteFrontierConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.NewFrontierDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.page.CollectionInfoPage;
import games.alejandrocoria.mapfrontiers.client.gui.screen.page.FrontierInfoPage;
import games.alejandrocoria.mapfrontiers.client.gui.screen.page.TerritoryListPage;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionLocalOverrides;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibility;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.fullscreen.IThemeButton;
import journeymap.api.v2.client.fullscreen.ModPopupMenu;
import journeymap.api.v2.client.fullscreen.ThemeButtonDisplay;
import journeymap.api.v2.client.util.UIState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class FullscreenMap {
    private enum ChunkDrawing {
        Nothing, Adding, Removing
    }

    private sealed interface SelectionCandidate permits FrontierSelectionCandidate, CollectionSelectionCandidate {
    }

    private record FrontierSelectionCandidate(FrontierOverlay frontier) implements SelectionCandidate {
    }

    private record CollectionSelectionCandidate(CollectionData collection) implements SelectionCandidate {
    }

    private final IClientAPI jmAPI;

    private FrontierOverlay frontierHighlighted;
    private @Nullable CollectionData selectedCollection;
    private @Nullable ResourceKey<Level> selectedCollectionHighlightDimension;

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
                cancelEditing();
                clearSelection();
                relocating = false;
                updateButtons();
            } else if (selectedCollection != null) {
                validateSelectedCollectionAvailability();
            }
        });

        MapFrontiersClient.getFrontierEvents().subscribeUpdated(this, (frontierOverlay, playerID) -> {
            if (frontierHighlighted != null && frontierHighlighted.getId().equals(frontierOverlay.getId())) {
                cancelEditing();
                frontierHighlighted = frontierOverlay;
                frontierHighlighted.setHighlighted(true);
                relocating = false;
                updateButtons();
            } else if (selectedCollection != null) {
                validateSelectedCollectionAvailability();
            }
        });

        MapFrontiersClient.getCollectionEvents().subscribeDeleted(this, collectionId -> {
            if (selectedCollection != null && selectedCollection.getId().equals(collectionId)) {
                clearSelection();
                updateButtons();
            }
        });

        MapFrontiersClient.getCollectionEvents().subscribeUpdated(this, collection -> {
            if (selectedCollection != null && selectedCollection.getId().equals(collection.getId())) {
                selectCollection(collection);
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

            if (!editing || drawingChunk == ChunkDrawing.Nothing || frontierHighlighted.getShape() != FrontierShape.Chunk) {
                return;
            }

            drawingChunk = ChunkDrawing.Nothing;
        });
    }

    public void close() {
        clearSelection();
        MapFrontiersClient.getFrontierEvents().unsubscribe(this);
        MapFrontiersClient.getCollectionEvents().unsubscribe(this);
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        ClientGlobalEvents.unsubscribeAllEvents(this);
    }

    public void addButtons(ThemeButtonDisplay buttonDisplay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        String path = "textures/gui/journeymap/";
        buttonFrontiers = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_mapfrontiers"), Identifier.fromNamespaceAndPath(MapFrontiers.MODID, path + "mapfrontiers.png"), b -> buttonFrontiersPressed());
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
            if (frontierHighlighted.getShape() == FrontierShape.Vertex) {
                popupMenu.addMenuItem(I18n.get("mapfrontiers.add_vertex"), this::buttonAddVertex);
                if (frontierHighlighted.getSelectedVertexIndex() != -1) {
                    popupMenu.addMenuItem(I18n.get("mapfrontiers.remove_vertex"), p -> buttonRemoveVertex());
                }
            } else if (frontierHighlighted.getShape() == FrontierShape.Path) {
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

            ModPopupMenu subMenu = popupMenu.createSubItemList("MapFrontiers");
            subMenu.addMenuItem(I18n.get("mapfrontiers.button_mapfrontiers"), p -> buttonFrontiersPressed());
            if (selectedCollection == null || canCreateFrontierInCollection(selectedCollection, playerUser)) {
                subMenu.addMenuItem(I18n.get("mapfrontiers.button_new_frontier"), p -> buttonNewPressed(p));
            }
            if (selectedCollection != null) {
                subMenu.addMenuItem(I18n.get("mapfrontiers.button_collection_info"), p -> buttonInfoPressed());
                if (canUpdateSelectedCollection(playerUser)) {
                    subMenu.addMenuItem(selectedCollection.getVisibilityData().isVisible()
                            ? I18n.get("mapfrontiers.button_hide_collection")
                            : I18n.get("mapfrontiers.button_show_collection"), p -> buttonVisibleToggled());
                }
                if (canDeleteSelectedCollection(playerUser)) {
                    subMenu.addMenuItem(I18n.get("mapfrontiers.button_delete_collection"), p -> buttonDelete());
                }
            } else if (frontierHighlighted != null) {
                SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontierHighlighted, playerUser);
                subMenu.addMenuItem(I18n.get("mapfrontiers.button_frontier_info"), p -> buttonInfoPressed());
                if (actions.canUpdate && frontierHighlighted.getVisibility(FrontierVisibility.Frontier)
                        && frontierHighlighted.getVisibility(FrontierVisibility.Fullscreen)) {
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
    }

    public void stopEditing() {
        finishEditing(true);
    }

    private void cancelEditing() {
        finishEditing(false);
    }

    private void finishEditing(boolean persistShapeChanges) {
        if (editing) {
            frontierHighlighted.endInteractiveEdit();
            editing = false;
            relocating = false;
            frontierHighlighted.clearSelectedEditablePoint();
            if (shapeDirty && persistShapeChanges) {
                FrontierChange change = new FrontierChange();
                change.setShape(frontierHighlighted.getVertices(), frontierHighlighted.getChunks(), frontierHighlighted.getPoints(),
                        frontierHighlighted.getShape());
                MapFrontiersClient.getOperationService().updateFrontier(frontierHighlighted, change);
            }
            shapeDirty = false;
        }

        if (buttonEdit != null) {
            buttonEdit.setToggled(false);
        }

        updateButtons();
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
        UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
        boolean selectedFrontierVisible = frontierHighlighted != null
                && uiState != null
                && frontierHighlighted.getDimension().equals(uiState.dimension)
                && frontierHighlighted.isVisibleOnFullscreenMap(uiState.mapType);
        boolean hasSelection = frontierHighlighted != null || selectedCollection != null;
        boolean canCreateInSelectedCollection = selectedCollection != null && canCreateFrontierInCollection(selectedCollection, playerUser);
        boolean canUpdateSelectedCollection = selectedCollection != null && canUpdateSelectedCollection(playerUser);
        boolean canDeleteSelectedCollection = selectedCollection != null && canDeleteSelectedCollection(playerUser);

        buttonFrontiers.setEnabled(!editing);
        buttonNew.setEnabled(!editing && (selectedCollection == null || canCreateInSelectedCollection));
        buttonInfo.setEnabled(hasSelection && !editing);
        buttonEdit.setEnabled(frontierHighlighted != null && SettingsProfile.getAvailableActions(profile, frontierHighlighted, playerUser).canUpdate
                && selectedFrontierVisible);
        buttonVisible.setEnabled(!editing && ((frontierHighlighted != null
                && SettingsProfile.getAvailableActions(profile, frontierHighlighted, playerUser).canUpdate)
                || canUpdateSelectedCollection));
        buttonDelete.setEnabled(!editing && ((frontierHighlighted != null
                && SettingsProfile.getAvailableActions(profile, frontierHighlighted, playerUser).canDelete)
                || canDeleteSelectedCollection));

        if (selectedCollection != null) {
            buttonInfo.getButton().setMessage(Component.translatable("mapfrontiers.button_collection_info"));
            buttonVisible.setLabels(I18n.get("mapfrontiers.button_hide_collection"), I18n.get("mapfrontiers.button_show_collection"));
            buttonDelete.getButton().setMessage(Component.translatable("mapfrontiers.button_delete_collection"));
        } else {
            buttonInfo.getButton().setMessage(Component.translatable("mapfrontiers.button_frontier_info"));
            buttonVisible.setLabels(I18n.get("mapfrontiers.button_hide_frontier"), I18n.get("mapfrontiers.button_show_frontier"));
            buttonDelete.getButton().setMessage(Component.translatable("mapfrontiers.button_delete_frontier"));
        }

        if (selectedCollection != null) {
            buttonVisible.setToggled(selectedCollection.getVisibilityData().isVisible());
        } else if (frontierHighlighted != null) {
            buttonVisible.setToggled(frontierHighlighted.getVisibilityData().getFrontier());
        } else {
            buttonVisible.setToggled(false);
        }
    }

    private void buttonFrontiersPressed() {
        new TerritoryListPage(jmAPI, this).display();
    }

    private void buttonNewPressed(BlockPos centerPos) {
        @Nullable CollectionData targetCollection = selectedCollection;
        @Nullable UUID targetCollectionId = targetCollection == null ? null : targetCollection.getId();
        @Nullable Boolean personal = targetCollection == null ? null : targetCollection.getPersonal();
        TerritoryLifetime lifetime = targetCollection != null && targetCollection.isSessionOnly()
                ? TerritoryLifetime.SESSION_ONLY
                : TerritoryLifetime.PERSISTENT;
        if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
            frontierHighlighted = null;
        }

        new NewFrontierDialog(jmAPI, centerPos, personal, lifetime, targetCollectionId, createNewFrontierResultHandler()).display();

        updateButtons();
    }

    private void buttonInfoPressed() {
        if (selectedCollection != null) {
            openCollectionInfo(selectedCollection);
        } else if (frontierHighlighted != null) {
            openFrontierInfo(frontierHighlighted);
        }
    }

    private void buttonEditToggled() {
        if (!editing) {
            startEditingSelectedFrontier();
        } else {
            stopEditing();
        }
    }

    private void buttonVisibleToggled() {
        if (selectedCollection != null) {
            CollectionVisibilityData visibilityData = new CollectionVisibilityData(selectedCollection.getVisibilityData());
            visibilityData.setVisible(!visibilityData.isVisible());
            selectedCollection.setVisibilityData(visibilityData);
            MapFrontiersClient.getOperationService().updateCollection(selectedCollection);
        } else if (frontierHighlighted != null) {
            frontierHighlighted.setVisibility(FrontierVisibility.Frontier,
                    !frontierHighlighted.getVisibilityData().getFrontier());
            FrontierChange change = new FrontierChange();
            change.setVisibility(frontierHighlighted.getVisibilityData());
            MapFrontiersClient.getOperationService().updateFrontier(frontierHighlighted, change);
        }

        updateButtons();
    }

    private void buttonDelete() {
        if (selectedCollection != null) {
            new DeleteCollectionConfirmationDialog(selectedCollection, ClientConfig.ASK_CONFIRMATION_COLLECTION_DELETE,
                    response -> deleteCollection()).display();
        } else if (frontierHighlighted != null) {
            new DeleteFrontierConfirmationDialog(frontierHighlighted, ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE,
                    response -> deleteFrontier()).display();
        } else {
            updateButtons();
        }
    }

    private void deleteFrontier() {
        if (editing) {
            stopEditing();
        }
        MapFrontiersClient.getOperationService().deleteFrontier(frontierHighlighted);
        clearSelection();
        updateButtons();
    }

    private void deleteCollection() {
        if (selectedCollection == null) {
            return;
        }

        MapFrontiersClient.getOperationService().deleteCollection(selectedCollection);
        clearSelection();
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
        return editing && frontierHighlighted.getShape() == FrontierShape.Vertex;
    }

    public boolean isEditingPaths() {
        return editing && frontierHighlighted.getShape() == FrontierShape.Path;
    }

    public boolean isEditingChunks() {
        return editing && frontierHighlighted.getShape() == FrontierShape.Chunk;
    }

    public FrontierOverlay getSelected() {
        return frontierHighlighted;
    }

    public @Nullable CollectionData getSelectedCollection() {
        return selectedCollection;
    }

    public void showCreatedFrontier(FrontierOverlay frontier) {
        stopEditing();
        selectFrontier(frontier);
    }

    public void openFrontierInfo(FrontierOverlay frontier) {
        showCreatedFrontier(frontier);
        new FrontierInfoPage(jmAPI, frontier).display();
    }

    public void beginEditingFrontier(FrontierOverlay frontier) {
        showCreatedFrontier(frontier);
        startEditingSelectedFrontier();
    }

    public NewFrontierDialog.ResultHandler createNewFrontierResultHandler() {
        return new NewFrontierDialog.ResultHandler() {
            @Override
            public void beforeCreate(NewFrontierDialog dialog, AfterCreatingFrontier action) {
                dialog.closeToFullscreenMap();
            }

            @Override
            public void onFrontierCreated(FrontierOverlay frontier, AfterCreatingFrontier action) {
                showCreatedFrontier(frontier);
                if (action == AfterCreatingFrontier.InfoScreen) {
                    openFrontierInfo(frontier);
                } else if (action == AfterCreatingFrontier.EditShape) {
                    beginEditingFrontier(frontier);
                }
            }
        };
    }

    public void selectFrontier(@Nullable FrontierOverlay frontier) {
        UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
        if (uiState != null && frontier != null && frontier.getDimension().equals(uiState.dimension)) {
            if (frontierHighlighted != frontier) {
                clearSelection();
                frontierHighlighted = frontier;
                frontierHighlighted.setHighlighted(true);
            }
        } else {
            clearSelection();
        }

        updateButtons();
    }

    public void selectCollection(@Nullable CollectionData collection) {
        UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
        if (uiState != null && collection != null && isCollectionAvailableInDimension(collection.getId(), uiState.dimension)) {
            clearSelection();
            selectedCollection = MapFrontiersClient.getCollection(collection.getId());
            if (selectedCollection == null) {
                selectedCollection = collection;
            }
            selectedCollectionHighlightDimension = uiState.dimension;
            MapFrontiersClient.setCollectionHighlighted(selectedCollection.getId(), uiState.dimension, true);
        } else {
            clearSelection();
        }

        updateButtons();
    }

    private void startEditingSelectedFrontier() {
        if (frontierHighlighted == null || editing) {
            updateButtons();
            return;
        }

        editing = true;
        shapeDirty = false;
        relocating = false;
        drawingChunk = ChunkDrawing.Nothing;
        frontierHighlighted.beginInteractiveEdit();
        frontierHighlighted.clearSelectedEditablePoint();

        if (buttonEdit != null) {
            buttonEdit.setToggled(true);
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
            else if (frontierHighlighted.getShape() == FrontierShape.Vertex) {
                frontierHighlighted.selectClosestVertex(position, maxDistanceToClosest);
            } else if (frontierHighlighted.getShape() == FrontierShape.Path) {
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

        if (ClientConfig.FRONTIER_VISIBILITY.get() == FrontierDisplayVisibility.Never) {
            clearSelection();
            updateButtons();
            return false;
        }

        List<FrontierOverlay> frontiers = MapFrontiersClient.getFrontiersInPosition(dimension, position, maxDistanceToClosest, uiState.mapType);
        List<SelectionCandidate> candidates = buildSelectionCandidates(frontiers, uiState);
        if (candidates.isEmpty()) {
            clearSelection();
            updateButtons();
        } else if (candidates.size() == 1 || getCurrentSelectionCandidate() == null) {
            selectCandidate(candidates.getFirst());
        } else {
            int i = indexOfCurrentSelection(candidates);
            i = (i + 1) % candidates.size();
            selectCandidate(candidates.get(i));
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

        if (frontierHighlighted.getShape() == FrontierShape.Chunk) {
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
            if (frontierHighlighted.getShape() == FrontierShape.Vertex) {
                if (!position.equals(relocatingPrevPos)) {
                    frontierHighlighted.moveAllVertices(position.subtract(relocatingPrevPos));
                    relocatingPrevPos = position;
                    shapeDirty = true;
                }
            } else if (frontierHighlighted.getShape() == FrontierShape.Path) {
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

        if (frontierHighlighted.getShape() != FrontierShape.Chunk) {
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

    private void openCollectionInfo(CollectionData collection) {
        selectCollection(collection);
        new CollectionInfoPage(collection).display();
    }

    private void clearSelection() {
        if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
            frontierHighlighted = null;
        }
        if (selectedCollection != null && selectedCollectionHighlightDimension != null) {
            MapFrontiersClient.setCollectionHighlighted(selectedCollection.getId(), selectedCollectionHighlightDimension, false);
        }
        selectedCollection = null;
        selectedCollectionHighlightDimension = null;
    }

    private void validateSelectedCollectionAvailability() {
        if (selectedCollection == null) {
            return;
        }

        UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
        if (uiState == null || !isCollectionAvailableInDimension(selectedCollection.getId(), uiState.dimension)) {
            clearSelection();
            updateButtons();
        }
    }

    private @Nullable SelectionCandidate getCurrentSelectionCandidate() {
        if (selectedCollection != null) {
            return new CollectionSelectionCandidate(selectedCollection);
        }
        if (frontierHighlighted != null) {
            return new FrontierSelectionCandidate(frontierHighlighted);
        }
        return null;
    }

    private void selectCandidate(SelectionCandidate candidate) {
        if (candidate instanceof CollectionSelectionCandidate collectionCandidate) {
            selectCollection(collectionCandidate.collection());
        } else if (candidate instanceof FrontierSelectionCandidate frontierCandidate) {
            selectFrontier(frontierCandidate.frontier());
        }
    }

    private int indexOfCurrentSelection(List<SelectionCandidate> candidates) {
        SelectionCandidate currentSelection = getCurrentSelectionCandidate();
        if (currentSelection == null) {
            return -1;
        }

        for (int i = 0; i < candidates.size(); ++i) {
            if (matchesSelection(candidates.get(i), currentSelection)) {
                return i;
            }
        }

        return -1;
    }

    private static boolean matchesSelection(SelectionCandidate left, SelectionCandidate right) {
        if (left instanceof FrontierSelectionCandidate leftFrontier && right instanceof FrontierSelectionCandidate rightFrontier) {
            return leftFrontier.frontier().getId().equals(rightFrontier.frontier().getId());
        }
        if (left instanceof CollectionSelectionCandidate leftCollection && right instanceof CollectionSelectionCandidate rightCollection) {
            return leftCollection.collection().getId().equals(rightCollection.collection().getId());
        }
        return false;
    }

    private List<SelectionCandidate> buildSelectionCandidates(List<FrontierOverlay> frontiers, UIState uiState) {
        if (frontiers.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<UUID, CollectionData> collectionsById = new LinkedHashMap<>();
        for (FrontierOverlay frontier : frontiers) {
            if (frontier.getShape() == FrontierShape.Path || frontier.getCollectionId() == null) {
                continue;
            }

            CollectionData collection = MapFrontiersClient.getCollection(frontier.getCollectionId());
            if (collection != null && isCollectionAvailableInDimension(collection.getId(), uiState.dimension)) {
                collectionsById.putIfAbsent(collection.getId(), collection);
            }
        }

        if (collectionsById.isEmpty()) {
            return new ArrayList<>(frontiers.stream().map(FrontierSelectionCandidate::new).toList());
        }

        List<CollectionData> visibleCollections = new ArrayList<>();
        List<CollectionData> hiddenCollections = new ArrayList<>();
        for (CollectionData collection : collectionsById.values()) {
            if (isCollectionVisibleAtCurrentZoom(collection, uiState)) {
                visibleCollections.add(collection);
            } else {
                hiddenCollections.add(collection);
            }
        }

        Comparator<CollectionData> byArea = Comparator.comparingDouble(collection -> resolveCollectionArea(collection, uiState.dimension));
        visibleCollections.sort(byArea);
        hiddenCollections.sort(byArea);

        List<SelectionCandidate> candidates = new ArrayList<>(visibleCollections.size() + frontiers.size() + hiddenCollections.size());
        visibleCollections.forEach(collection -> candidates.add(new CollectionSelectionCandidate(collection)));
        frontiers.forEach(frontier -> candidates.add(new FrontierSelectionCandidate(frontier)));
        hiddenCollections.forEach(collection -> candidates.add(new CollectionSelectionCandidate(collection)));
        return candidates;
    }

    private boolean isCollectionVisibleAtCurrentZoom(CollectionData collection, UIState uiState) {
        if (!resolveCollectionVisibility(collection)) {
            return false;
        }

        int zoom = resolveCollectionMaxZoom(collection);
        return CollectionVisibilityData.isZoomEnabled(zoom) && uiState.zoom <= zoom;
    }

    private int resolveCollectionMaxZoom(CollectionData collection) {
        var visibilityOverride = MapFrontiersClient.getCollectionLocalOverrides().getVisibility(collection.getId());
        CollectionVisibilityData resolvedVisibility = CollectionLocalOverrides.resolveVisibility(collection.getVisibilityData(), visibilityOverride);

        return ClientConfig.COLLECTION_FULLSCREEN_ZOOM_FORCED.get()
                ? ClientConfig.getNormalizedCollectionFullscreenZoom()
                : resolvedVisibility.getFullscreenZoom();
    }

    private boolean resolveCollectionVisibility(CollectionData collection) {
        var visibilityOverride = MapFrontiersClient.getCollectionLocalOverrides().getVisibility(collection.getId());
        CollectionVisibilityData resolvedVisibility = CollectionLocalOverrides.resolveVisibility(collection.getVisibilityData(), visibilityOverride);
        return ClientConfig.resolveVisibilityValue(ClientConfig.COLLECTION_VISIBILITY.get(), resolvedVisibility.isVisible());
    }

    private double resolveCollectionArea(CollectionData collection, ResourceKey<Level> dimension) {
        double totalArea = 0.0;
        for (FrontierOverlay frontier : MapFrontiersClient.getFrontiersInCollection(collection.getId(), dimension)) {
            if (frontier.getShape() != FrontierShape.Path) {
                totalArea += frontier.area;
            }
        }
        return totalArea;
    }

    private boolean isCollectionAvailableInDimension(UUID collectionId, ResourceKey<Level> dimension) {
        for (FrontierOverlay frontier : MapFrontiersClient.getFrontiersInCollection(collectionId, dimension)) {
            if (frontier.getShape() != FrontierShape.Path) {
                return true;
            }
        }
        return false;
    }

    private boolean canCreateFrontierInCollection(CollectionData collection, SettingsUser playerUser) {
        if (collection.getPersonal()) {
            return collection.getOwner().equals(playerUser);
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        return profile != null && profile.createFrontier == SettingsProfile.State.Enabled;
    }

    private boolean canUpdateSelectedCollection(SettingsUser playerUser) {
        if (selectedCollection == null) {
            return false;
        }

        return SettingsProfile.canUpdateCollection(MapFrontiersClient.getSettingsProfile(), selectedCollection, playerUser);
    }

    private boolean canDeleteSelectedCollection(SettingsUser playerUser) {
        if (selectedCollection == null) {
            return false;
        }

        if (selectedCollection.getPersonal()) {
            return selectedCollection.getOwner().equals(playerUser);
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        return profile != null && (profile.deleteFrontier == SettingsProfile.State.Enabled
                || (profile.deleteFrontier == SettingsProfile.State.Owner && selectedCollection.getOwner().equals(playerUser)));
    }
}
