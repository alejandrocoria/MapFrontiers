package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.plugin.MapFrontiersPlugin;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChangeApplicationResult;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibility;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class FrontiersOverlayManager {
    private static final int REGION_BUCKET_SIZE_BLOCKS = 512;
    private static final int MAX_REGION_BUCKETS_PER_FRONTIER = 4096;

    private final IClientAPI jmAPI;
    private final HashMap<ResourceKey<Level>, ArrayList<FrontierOverlay>> dimensionsFrontiers;
    private final HashMap<UUID, FrontierOverlay> frontiersById;
    private final HashMap<UUID, FrontierOverlay> frontiersByCopiedFromId;
    private final HashMap<UUID, HashSet<FrontierOverlay>> frontiersByCollectionId;
    private final HashMap<ResourceKey<Level>, HashMap<Long, HashSet<FrontierOverlay>>> frontiersByRegionBucket;
    private final HashMap<ResourceKey<Level>, LinkedHashSet<FrontierOverlay>> oversizedFrontiersByDimension;
    private final HashMap<UUID, UUID> frontierCopiedFromIdsById;
    private final HashMap<UUID, UUID> frontierCollectionIdsById;
    private final HashMap<UUID, Set<Long>> frontierRegionBucketsById;
    private final HashMap<UUID, ResourceKey<Level>> oversizedFrontierDimensionsById;
    private final LinkedHashSet<FrontierOverlay> dirtyFrontiers;
    private final SelectedEditablePointMarker selectedEditablePointMarker;

    public FrontiersOverlayManager(IClientAPI jmAPI) {
        this.jmAPI = jmAPI;
        dimensionsFrontiers = new HashMap<>();
        frontiersById = new HashMap<>();
        frontiersByCopiedFromId = new HashMap<>();
        frontiersByCollectionId = new HashMap<>();
        frontiersByRegionBucket = new HashMap<>();
        oversizedFrontiersByDimension = new HashMap<>();
        frontierCopiedFromIdsById = new HashMap<>();
        frontierCollectionIdsById = new HashMap<>();
        frontierRegionBucketsById = new HashMap<>();
        oversizedFrontierDimensionsById = new HashMap<>();
        dirtyFrontiers = new LinkedHashSet<>();
        selectedEditablePointMarker = new SelectedEditablePointMarker(jmAPI);

        ClientGlobalEvents.subscribeClientTickEvent(this, client -> selectedEditablePointMarker.tick(
                client.getDeltaTracker().getGameTimeDeltaTicks(), MapFrontiersPlugin.isEditing()));
        ClientGlobalEvents.subscribeUpdatedConfigEvent(this, () -> {
            selectedEditablePointMarker.configUpdated();
            rebuildAllOverlaysNow();
        });
    }

    public void close() {
        ClientGlobalEvents.unsubscribeAllEvents(this);
        clearSelectedEditablePointMarker();
        clearFrontiers();
    }

    public FrontierOverlay addFrontier(FrontierData data) {
        FrontierOverlay frontierOverlay = new FrontierOverlay(data, jmAPI);
        addFrontier(frontierOverlay);
        return frontierOverlay;
    }

    public void addFrontier(FrontierOverlay frontierOverlay) {
        List<FrontierOverlay> frontiers = getAllFrontiers(frontierOverlay.getDimension());
        frontiers.add(frontierOverlay);
        registerFrontier(frontierOverlay);
        MapFrontiersClient.markFrontierActivationDirty();
    }

    public FrontierOverlay deleteFrontier(UUID id) {
        FrontierOverlay frontier = frontiersById.get(id);
        return frontier == null ? null : deleteFrontier(frontier.getDimension(), id);
    }

    public FrontierOverlay deleteFrontier(ResourceKey<Level> dimension, UUID id) {
        FrontierOverlay frontier = frontiersById.get(id);
        if (frontier == null || !frontier.getDimension().equals(dimension)) {
            return null;
        }

        List<FrontierOverlay> frontiers = getAllFrontiers(dimension);
        if (!frontiers.remove(frontier)) {
            return null;
        }

        unregisterFrontier(frontier);
        deleteFrontierOverlay(frontier);
        MapFrontiersClient.markFrontierActivationDirty();

        return frontier;
    }

    @Nullable
    public FrontierChangeApplicationResult applyFrontierChange(ResourceKey<Level> dimension, UUID id, FrontierChange change) {
        FrontierOverlay frontierOverlay = frontiersById.get(id);
        if (frontierOverlay == null || !frontierOverlay.getDimension().equals(dimension)) {
            return null;
        }

        FrontierChangeApplicationResult result = frontierOverlay.applyChange(change);
        if (!result.isApplied()) {
            return result;
        }

        FrontierChange effectiveChange = result.effectiveChange();
        if (effectiveChange.affectsGeometry() || effectiveChange.hasCollectionIdChange()) {
            refreshFrontierDerivedIndexes(frontierOverlay);
        }
        MapFrontiersClient.markFrontierActivationDirty();
        return FrontierChangeApplicationResult.applied(frontierOverlay, effectiveChange);
    }

    @Nullable
    public FrontierOverlay applyFrontierSharingChange(ResourceKey<Level> dimension, UUID id, FrontierSharingChange sharingChange) {
        FrontierOverlay frontierOverlay = frontiersById.get(id);
        if (frontierOverlay == null || !frontierOverlay.getDimension().equals(dimension)) {
            return null;
        }

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
        try {
            for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
                for (FrontierOverlay frontier : frontiers) {
                    deleteFrontierOverlay(frontier);
                }
            }
        } finally {
            dimensionsFrontiers.clear();
            frontiersById.clear();
            frontiersByCopiedFromId.clear();
            frontiersByCollectionId.clear();
            frontiersByRegionBucket.clear();
            oversizedFrontiersByDimension.clear();
            frontierCopiedFromIdsById.clear();
            frontierCollectionIdsById.clear();
            frontierRegionBucketsById.clear();
            oversizedFrontierDimensionsById.clear();
            dirtyFrontiers.clear();
            MapFrontiersClient.markFrontierActivationDirty();
        }
    }

    public List<FrontierOverlay> getCandidateFrontiersInBounds(ResourceKey<Level> dimension, int minX, int maxX, int minZ, int maxZ) {
        LinkedHashSet<FrontierOverlay> candidates = new LinkedHashSet<>();
        HashMap<Long, HashSet<FrontierOverlay>> regionBuckets = frontiersByRegionBucket.get(dimension);
        if (regionBuckets != null) {
            int minRegionX = Math.floorDiv(minX, REGION_BUCKET_SIZE_BLOCKS);
            int maxRegionX = Math.floorDiv(maxX, REGION_BUCKET_SIZE_BLOCKS);
            int minRegionZ = Math.floorDiv(minZ, REGION_BUCKET_SIZE_BLOCKS);
            int maxRegionZ = Math.floorDiv(maxZ, REGION_BUCKET_SIZE_BLOCKS);

            for (int regionX = minRegionX; regionX <= maxRegionX; ++regionX) {
                for (int regionZ = minRegionZ; regionZ <= maxRegionZ; ++regionZ) {
                    HashSet<FrontierOverlay> frontiers = regionBuckets.get(getRegionBucketKey(regionX, regionZ));
                    if (frontiers != null) {
                        candidates.addAll(frontiers);
                    }
                }
            }
        }

        LinkedHashSet<FrontierOverlay> oversizedFrontiers = oversizedFrontiersByDimension.get(dimension);
        if (oversizedFrontiers != null) {
            candidates.addAll(oversizedFrontiers);
        }

        if (candidates.isEmpty()) {
            return List.of();
        }

        return new ArrayList<>(candidates);
    }

    public List<FrontierOverlay> getFrontiersInPosition(ResourceKey<Level> dimension, BlockPos pos, double maxDistanceToOpen,
                                                        @Nullable Context.MapType fullscreenMapType) {
        List<FrontierOverlay> frontiersInPosition = new ArrayList<>();
        int radius = (int) Math.ceil(Math.max(0.0, maxDistanceToOpen));
        for (FrontierOverlay frontier : getCandidateFrontiersInBounds(dimension,
                pos.getX() - radius, pos.getX() + radius,
                pos.getZ() - radius, pos.getZ() + radius)) {
            if (!frontier.isInsideBoundingBox(pos, maxDistanceToOpen)) {
                continue;
            }

            boolean visible = fullscreenMapType == null
                    ? frontier.getVisibility(FrontierVisibility.Frontier)
                    : frontier.isVisibleOnFullscreenMap(fullscreenMapType);
            if (visible && frontier.pointIsInside(pos, maxDistanceToOpen)) {
                frontiersInPosition.add(frontier);
            }
        }

        return frontiersInPosition;
    }

    @Nullable
    public FrontierOverlay getFrontierCopiedFrom(UUID copiedFromId) {
        return frontiersByCopiedFromId.get(copiedFromId);
    }

    @Nullable
    public FrontierOverlay getFrontier(UUID frontierId) {
        return frontiersById.get(frontierId);
    }

    public void refreshFrontierDerivedIndexes(FrontierOverlay frontier) {
        FrontierOverlay indexedFrontier = frontiersById.get(frontier.getId());
        if (indexedFrontier != frontier) {
            return;
        }

        unregisterFrontierDerivedIndexes(frontier);
        registerFrontierDerivedIndexes(frontier);
    }

    public void processDirtyOverlays() {
        if (dirtyFrontiers.isEmpty()) {
            return;
        }

        List<FrontierOverlay> dirtyFrontiersSnapshot = new ArrayList<>(dirtyFrontiers);
        dirtyFrontiers.clear();
        for (FrontierOverlay frontier : dirtyFrontiersSnapshot) {
            if (frontiersById.get(frontier.getId()) == frontier) {
                frontier.processDirtyOverlay();
            }
        }
    }

    public void rebuildAllOverlaysNow() {
        dirtyFrontiers.clear();
        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                frontier.rebuildOverlayNow();
            }
        }
    }

    public void markCollectionPresentationDirty(UUID collectionId) {
        Set<FrontierOverlay> frontiers = frontiersByCollectionId.get(collectionId);
        if (frontiers == null) {
            return;
        }

        for (FrontierOverlay frontier : frontiers) {
            if (collectionId.equals(frontier.getCollectionId())) {
                frontier.markCollectionPresentationDirty();
            }
        }
    }

    public void updateSelectedMarker(ResourceKey<Level> dimension, @Nullable FrontierOverlay frontier) {
        BlockPos pos = frontier != null ? frontier.getSelectedEditablePoint() : null;
        selectedEditablePointMarker.update(dimension, pos);
    }

    private void clearSelectedEditablePointMarker() {
        try {
            selectedEditablePointMarker.clear();
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to clear selected frontier marker", t);
        }
    }

    private static void deleteFrontierOverlay(FrontierOverlay frontier) {
        try {
            frontier.deleted();
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to delete frontier overlay {}", frontier.getId(), t);
        }
    }

    private void registerFrontier(FrontierOverlay frontier) {
        frontiersById.put(frontier.getId(), frontier);
        frontier.setDirtyOverlayListener(() -> markFrontierDirty(frontier));
        registerFrontierDerivedIndexes(frontier);
    }

    private void unregisterFrontier(FrontierOverlay frontier) {
        unregisterFrontierDerivedIndexes(frontier);
        dirtyFrontiers.remove(frontier);
        frontier.setDirtyOverlayListener(null);
        frontiersById.remove(frontier.getId(), frontier);
    }

    private void markFrontierDirty(FrontierOverlay frontier) {
        if (frontiersById.get(frontier.getId()) == frontier) {
            dirtyFrontiers.add(frontier);
        }
    }

    private void registerFrontierDerivedIndexes(FrontierOverlay frontier) {
        UUID frontierId = frontier.getId();

        if (frontier.wasCopied()) {
            UUID copiedFromId = frontier.getCopiedFromId();
            frontiersByCopiedFromId.put(copiedFromId, frontier);
            frontierCopiedFromIdsById.put(frontierId, copiedFromId);
        }

        UUID collectionId = frontier.getCollectionId();
        if (collectionId != null) {
            frontiersByCollectionId.computeIfAbsent(collectionId, key -> new HashSet<>()).add(frontier);
            frontierCollectionIdsById.put(frontierId, collectionId);
        }

        RegionBucketCoverage regionBucketCoverage = computeRegionBucketCoverage(frontier);
        if (regionBucketCoverage == null) {
            return;
        }

        if (regionBucketCoverage.bucketCount() > MAX_REGION_BUCKETS_PER_FRONTIER) {
            ResourceKey<Level> dimension = frontier.getDimension();
            oversizedFrontiersByDimension.computeIfAbsent(dimension, key -> new LinkedHashSet<>()).add(frontier);
            oversizedFrontierDimensionsById.put(frontierId, dimension);
            return;
        }

        Set<Long> regionBucketKeys = computeRegionBucketKeys(regionBucketCoverage);
        frontierRegionBucketsById.put(frontierId, regionBucketKeys);

        HashMap<Long, HashSet<FrontierOverlay>> regionBuckets = frontiersByRegionBucket.computeIfAbsent(frontier.getDimension(), key -> new HashMap<>());
        for (Long regionBucketKey : regionBucketKeys) {
            regionBuckets.computeIfAbsent(regionBucketKey, key -> new HashSet<>()).add(frontier);
        }
    }

    private void unregisterFrontierDerivedIndexes(FrontierOverlay frontier) {
        UUID frontierId = frontier.getId();

        UUID copiedFromId = frontierCopiedFromIdsById.remove(frontierId);
        if (copiedFromId != null) {
            frontiersByCopiedFromId.remove(copiedFromId, frontier);
        }

        UUID collectionId = frontierCollectionIdsById.remove(frontierId);
        if (collectionId != null) {
            HashSet<FrontierOverlay> frontiers = frontiersByCollectionId.get(collectionId);
            if (frontiers != null) {
                frontiers.remove(frontier);
                if (frontiers.isEmpty()) {
                    frontiersByCollectionId.remove(collectionId);
                }
            }
        }

        ResourceKey<Level> oversizedDimension = oversizedFrontierDimensionsById.remove(frontierId);
        if (oversizedDimension != null) {
            LinkedHashSet<FrontierOverlay> frontiers = oversizedFrontiersByDimension.get(oversizedDimension);
            if (frontiers != null) {
                frontiers.remove(frontier);
                if (frontiers.isEmpty()) {
                    oversizedFrontiersByDimension.remove(oversizedDimension);
                }
            }
            return;
        }

        Set<Long> regionBucketKeys = frontierRegionBucketsById.remove(frontierId);
        if (regionBucketKeys == null || regionBucketKeys.isEmpty()) {
            return;
        }

        HashMap<Long, HashSet<FrontierOverlay>> regionBuckets = frontiersByRegionBucket.get(frontier.getDimension());
        if (regionBuckets == null) {
            return;
        }

        for (Long regionBucketKey : regionBucketKeys) {
            HashSet<FrontierOverlay> frontiers = regionBuckets.get(regionBucketKey);
            if (frontiers != null) {
                frontiers.remove(frontier);
                if (frontiers.isEmpty()) {
                    regionBuckets.remove(regionBucketKey);
                }
            }
        }

        if (regionBuckets.isEmpty()) {
            frontiersByRegionBucket.remove(frontier.getDimension());
        }
    }

    @Nullable
    private static RegionBucketCoverage computeRegionBucketCoverage(FrontierOverlay frontier) {
        if (frontier.topLeft == null || frontier.bottomRight == null) {
            return null;
        }

        int minX = frontier.topLeft.getX();
        int minZ = frontier.topLeft.getZ();
        int maxX = frontier.bottomRight.getX();
        int maxZ = frontier.bottomRight.getZ();
        if (frontier.getShape() == FrontierShape.Chunk) {
            maxX -= 1;
            maxZ -= 1;
        }

        int minRegionX = Math.floorDiv(minX, REGION_BUCKET_SIZE_BLOCKS);
        int maxRegionX = Math.floorDiv(maxX, REGION_BUCKET_SIZE_BLOCKS);
        int minRegionZ = Math.floorDiv(minZ, REGION_BUCKET_SIZE_BLOCKS);
        int maxRegionZ = Math.floorDiv(maxZ, REGION_BUCKET_SIZE_BLOCKS);
        long bucketCount = (long) (maxRegionX - minRegionX + 1) * (maxRegionZ - minRegionZ + 1);

        return new RegionBucketCoverage(minRegionX, maxRegionX, minRegionZ, maxRegionZ, bucketCount);
    }

    private static Set<Long> computeRegionBucketKeys(RegionBucketCoverage coverage) {
        HashSet<Long> regionBucketKeys = new HashSet<>();
        for (int regionX = coverage.minRegionX(); regionX <= coverage.maxRegionX(); ++regionX) {
            for (int regionZ = coverage.minRegionZ(); regionZ <= coverage.maxRegionZ(); ++regionZ) {
                regionBucketKeys.add(getRegionBucketKey(regionX, regionZ));
            }
        }

        return regionBucketKeys;
    }

    private static long getRegionBucketKey(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    private record RegionBucketCoverage(int minRegionX,
                                        int maxRegionX,
                                        int minRegionZ,
                                        int maxRegionZ,
                                        long bucketCount) {
    }

}
