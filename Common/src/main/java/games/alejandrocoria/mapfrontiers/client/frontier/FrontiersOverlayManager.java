package games.alejandrocoria.mapfrontiers.client.frontier;

import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.plugin.MapFrontiersPlugin;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.util.ContainerHelper;
import journeymap.api.v2.client.IClientAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class FrontiersOverlayManager {
    private final IClientAPI jmAPI;
    private final HashMap<ResourceKey<Level>, ArrayList<FrontierOverlay>> dimensionsFrontiers;
    private final SelectedEditablePointMarker selectedEditablePointMarker;

    public FrontiersOverlayManager(IClientAPI jmAPI) {
        this.jmAPI = jmAPI;
        dimensionsFrontiers = new HashMap<>();
        selectedEditablePointMarker = new SelectedEditablePointMarker(jmAPI);

        ClientGlobalEvents.subscribeClientTickEvent(this, client -> selectedEditablePointMarker.tick(
                client.getDeltaTracker().getGameTimeDeltaTicks(), MapFrontiersPlugin.isEditing()));
        ClientGlobalEvents.subscribeUpdatedConfigEvent(this, () -> updateAllOverlays(true));
    }

    public void close() {
        ClientGlobalEvents.unsubscribeAllEvents(this);
        selectedEditablePointMarker.clear();

        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                frontier.deleted();
            }
        }

        dimensionsFrontiers.clear();
    }

    public FrontierOverlay addFrontier(FrontierData data) {
        List<FrontierOverlay> frontiers = getAllFrontiers(data.getDimension());

        FrontierOverlay frontierOverlay = new FrontierOverlay(data, jmAPI);
        frontiers.add(frontierOverlay);

        return frontierOverlay;
    }

    public void addFrontier(FrontierOverlay frontierOverlay) {
        List<FrontierOverlay> frontiers = getAllFrontiers(frontierOverlay.getDimension());
        frontiers.add(frontierOverlay);
    }

    public FrontierOverlay deleteFrontier(UUID id) {
        for (ResourceKey<Level> dimension : dimensionsFrontiers.keySet()) {
            FrontierOverlay frontierOverlay = deleteFrontier(dimension, id);
            if (frontierOverlay != null) {
                return frontierOverlay;
            }
        }

        return null;
    }

    public FrontierOverlay deleteFrontier(ResourceKey<Level> dimension, UUID id) {
        List<FrontierOverlay> frontiers = getAllFrontiers(dimension);

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(id));

        if (index < 0) {
            return null;
        }

        FrontierOverlay frontier = frontiers.remove(index);
        frontier.deleted();

        return frontier;
    }

    @Nullable
    public FrontierOverlay applyFrontierChange(ResourceKey<Level> dimension, UUID id, FrontierChange change) {
        List<FrontierOverlay> frontiers = getAllFrontiers(dimension);

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(id));
        if (index < 0) {
            return null;
        }

        FrontierOverlay frontierOverlay = frontiers.get(index);
        frontierOverlay.applyChange(change);
        return frontierOverlay;
    }

    @Nullable
    public FrontierOverlay applyFrontierSharingChange(ResourceKey<Level> dimension, UUID id, FrontierSharingChange sharingChange) {
        List<FrontierOverlay> frontiers = getAllFrontiers(dimension);

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(id));
        if (index < 0) {
            return null;
        }

        FrontierOverlay frontierOverlay = frontiers.get(index);
        frontierOverlay.applySharingChange(sharingChange);
        return frontierOverlay;
    }

    public Map<ResourceKey<Level>, ArrayList<FrontierOverlay>> getAllFrontiers() {
        return dimensionsFrontiers;
    }

    public List<FrontierOverlay> getAllFrontiers(ResourceKey<Level> dimension) {
        return dimensionsFrontiers.computeIfAbsent(dimension, k -> new ArrayList<>());
    }

    public void replaceFrontiers(List<FrontierData> frontiers) {
        clearFrontiers();
        for (FrontierData data : frontiers) {
            addFrontier(data);
        }
    }

    public void clearFrontiers() {
        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                frontier.deleted();
            }
        }
        dimensionsFrontiers.clear();
    }

    public List<FrontierOverlay> getFrontiersInPosition(ResourceKey<Level> dimension, BlockPos pos, double maxDistanceToOpen) {
        List<FrontierOverlay> frontiersInPosition = new ArrayList<>();
        ArrayList<FrontierOverlay> frontiers = dimensionsFrontiers.get(dimension);
        if (frontiers != null) {
            for (FrontierOverlay frontier : frontiers) {
                if (frontier.getVisibility(FrontierData.VisibilityData.Visibility.Frontier) && frontier.pointIsInside(pos, maxDistanceToOpen)) {
                    frontiersInPosition.add(frontier);
                }
            }
        }

        return frontiersInPosition;
    }

    @Nullable
    public FrontierOverlay getFrontierCopiedFrom(UUID copiedFromId) {

        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                if (frontier.wasCopied() && frontier.getCopiedFromId().equals(copiedFromId)) {
                    return frontier;
                }
            }
        }
        return null;
    }

    @Nullable
    public FrontierOverlay getFrontier(UUID frontierId) {
        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                if (frontier.getId().equals(frontierId)) {
                    return frontier;
                }
            }
        }

        return null;
    }

    public void updateAllOverlays(boolean forceUpdate) {
        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                if (forceUpdate) {
                    frontier.updateOverlay();
                } else {
                    frontier.updateOverlayIfNeeded();
                }
            }
        }
    }

    public void updateSelectedMarker(ResourceKey<Level> dimension, @Nullable FrontierOverlay frontier) {
        BlockPos pos = frontier != null ? frontier.getSelectedEditablePoint() : null;
        selectedEditablePointMarker.update(dimension, pos);
    }

}
