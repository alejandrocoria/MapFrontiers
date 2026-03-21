package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.ConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.DeleteConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.FrontierInfo;
import games.alejandrocoria.mapfrontiers.client.gui.screen.FrontierList;
import games.alejandrocoria.mapfrontiers.client.gui.screen.NewFrontier;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
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

                if (Config.afterCreatingFrontier == Config.AfterCreatingFrontier.Edit) {
                    buttonEditToggled();
                } else if (Config.afterCreatingFrontier == Config.AfterCreatingFrontier.Info) {
                    buttonInfoPressed();
                }
            }
        });

        MapFrontiersClient.getFrontierEvents().subscribeUpdated(this, (frontierOverlay, playerID) -> {
            if (frontierHighlighted != null && frontierHighlighted.getId().equals(frontierOverlay.getId())) {
                frontierHighlighted = frontierOverlay;
                frontierHighlighted.setHighlighted(true);
                editing = false;
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
            if (actions.canUpdate && frontierHighlighted.getVisibility(FrontierData.VisibilityData.Visibility.Frontier) && frontierHighlighted.getVisibility(FrontierData.VisibilityData.Visibility.Fullscreen)) {
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
            MapFrontiersClient.getOperationService().updateFrontier(frontierHighlighted);
        }
    }

    public void updateButtons() {
        Player player = Minecraft.getInstance().player;

        if (buttonInfo == null || player == null) {
            return;
        }

        buttonFrontiers.setDrawButton(Config.fullscreenButtons);
        buttonNew.setDrawButton(Config.fullscreenButtons);
        buttonInfo.setDrawButton(Config.fullscreenButtons);
        buttonEdit.setDrawButton(Config.fullscreenButtons);
        buttonVisible.setDrawButton(Config.fullscreenButtons);
        buttonDelete.setDrawButton(Config.fullscreenButtons);

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(player);
        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontierHighlighted, playerUser);

        buttonFrontiers.setEnabled(!editing);
        buttonNew.setEnabled(!editing);
        buttonInfo.setEnabled(frontierHighlighted != null && !editing);
        buttonEdit.setEnabled(actions.canUpdate && frontierHighlighted.getVisibility(FrontierData.VisibilityData.Visibility.Frontier) && frontierHighlighted.getVisibility(FrontierData.VisibilityData.Visibility.Fullscreen));
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
            drawingChunk = ChunkDrawing.Nothing;
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
        if (Config.askConfirmationFrontierDelete) {
            new DeleteConfirmationDialog(
                    "mapfrontiers.delete_frontier_dialog",
                    response -> {
                        if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                            Config.askConfirmationFrontierDelete = false;
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

        updateButtons();
    }

    private void buttonRemoveVertex() {
        frontierHighlighted.removeSelectedVertex();

        updateButtons();
    }

    private void buttonRemoveConnected(List<ChunkPos> chunks) {
        for (ChunkPos chunk : chunks) {
            frontierHighlighted.removeChunk(chunk);
        }
    }

    private void buttonFillRegion(List<ChunkPos> chunks) {
        for (ChunkPos chunk : chunks) {
            frontierHighlighted.addChunk(chunk);
        }
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
            if (ScreenHelper.hasControlDown() && button == 1) {
                relocating = true;
                relocatingPrevPos = position;
                return true;
            }
            else if (frontierHighlighted.getMode() == FrontierData.Mode.Vertex) {
                frontierHighlighted.selectClosestVertex(position, maxDistanceToClosest);
            } else if (button == 1) {
                lastEditedChunk = new ChunkPos(position);
                if (ScreenHelper.hasShiftDown()) {
                    return false;
                }else {
                    if (frontierHighlighted.toggleChunk(lastEditedChunk)) {
                        drawingChunk = ChunkDrawing.Adding;
                    } else {
                        drawingChunk = ChunkDrawing.Removing;
                    }
                }
                return true;
            }
            return false;
        }

        if (Config.frontierVisibility == Config.Visibility.Never) {
            return false;
        }

        List<FrontierOverlay> frontiers = MapFrontiersClient.getFrontiersInPosition(dimension, position, maxDistanceToClosest);
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

        if (frontierHighlighted.getSelectedVertexIndex() == -1) {
            return false;
        }

        float snapDistance = 512.f / uiState.zoom * Config.snapDistance;
        frontierHighlighted.moveSelectedVertex(position, snapDistance);
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
                }
            } else {
                ChunkPos chunkPos = new ChunkPos(position);
                ChunkPos prevChunkPos = new ChunkPos(relocatingPrevPos);
                if (!chunkPos.equals(prevChunkPos)) {
                    frontierHighlighted.moveAllChunks(new ChunkPos(chunkPos.x - prevChunkPos.x, chunkPos.z - prevChunkPos.z));
                    relocatingPrevPos = position;
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
