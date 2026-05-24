package games.alejandrocoria.mapfrontiers.client.territory.collection;

import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierShape;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class CollectionOverlayManager {
    private final ClientCollectionRuntime collectionRuntime;
    private final LinkedHashMap<CollectionOverlayKey, CollectionOverlay> overlaysByKey = new LinkedHashMap<>();
    private final LinkedHashSet<CollectionOverlay> dirtyOverlays = new LinkedHashSet<>();

    public CollectionOverlayManager(ClientCollectionRuntime collectionRuntime) {
        this.collectionRuntime = collectionRuntime;
        ClientGlobalEvents.subscribeUpdatedConfigEvent(this, this::rebuildAllOverlaysNow);
    }

    public void close() {
        ClientGlobalEvents.unsubscribeAllEvents(this);
        clearOverlays();
    }

    public void syncFromCurrentRuntime() {
        LinkedHashSet<UUID> collectionIds = new LinkedHashSet<>();
        for (CollectionData collection : collectionRuntime.getCollections(CollectionScope.GLOBAL_PERSISTENT)) {
            collectionIds.add(collection.getId());
        }
        for (CollectionData collection : collectionRuntime.getCollections(CollectionScope.PERSONAL_PERSISTENT)) {
            collectionIds.add(collection.getId());
        }
        for (CollectionData collection : collectionRuntime.getCollections(CollectionScope.PERSONAL_SESSION)) {
            collectionIds.add(collection.getId());
        }
        for (CollectionOverlayKey key : overlaysByKey.keySet()) {
            collectionIds.add(key.collectionId());
        }

        for (UUID collectionId : collectionIds) {
            refreshCollectionDimensions(collectionId);
        }
    }

    public void processDirtyOverlays() {
        if (dirtyOverlays.isEmpty()) {
            return;
        }

        List<CollectionOverlay> dirtyOverlaysSnapshot = List.copyOf(dirtyOverlays);
        dirtyOverlays.clear();
        for (CollectionOverlay overlay : dirtyOverlaysSnapshot) {
            if (overlaysByKey.get(overlay.getKey()) == overlay) {
                overlay.processDirtyOverlay();
            }
        }
    }

    public void rebuildAllOverlaysNow() {
        dirtyOverlays.clear();
        for (CollectionOverlay overlay : overlaysByKey.values()) {
            overlay.rebuildOverlayNow();
        }
    }

    public void markCollectionDirty(UUID collectionId) {
        refreshCollectionDimensions(collectionId);
    }

    public void markFrontierMembershipDirty(FrontierOverlay frontier) {
        UUID collectionId = frontier.getCollectionId();
        if (collectionId != null) {
            refreshCollectionOverlay(collectionId, frontier.getDimension());
        }
    }

    public void markFrontierGeometryDirty(FrontierOverlay frontier, @Nullable UUID previousCollectionId) {
        UUID currentCollectionId = frontier.getCollectionId();
        ResourceKey<Level> dimension = frontier.getDimension();

        if (currentCollectionId != null) {
            refreshCollectionOverlay(currentCollectionId, dimension);
        }

        if (previousCollectionId != null && !previousCollectionId.equals(currentCollectionId)) {
            refreshCollectionOverlay(previousCollectionId, dimension);
        }
    }

    public void refreshCollectionOverlay(UUID collectionId, ResourceKey<Level> dimension) {
        CollectionOverlayKey key = new CollectionOverlayKey(collectionId, dimension);
        CollectionData collection = collectionRuntime.getCollection(collectionId);
        List<FrontierOverlay> eligibleMembers = getEligibleMembers(collectionId, dimension);

        if (collection == null || !collection.isCollectionViewEnabled() || eligibleMembers.isEmpty()) {
            deleteOverlay(key);
            return;
        }

        CollectionOverlay overlay = overlaysByKey.get(key);
        if (overlay == null) {
            overlay = new CollectionOverlay(key, collection, eligibleMembers);
            registerOverlay(overlay);
            overlay.rebuildOverlayNow();
            return;
        }

        overlay.refreshMembersAndCollection(collection, eligibleMembers);
    }

    private void refreshCollectionDimensions(UUID collectionId) {
        LinkedHashSet<ResourceKey<Level>> dimensions = new LinkedHashSet<>();
        for (FrontierOverlay frontier : collectionRuntime.getFrontiersInCollection(collectionId)) {
            if (isEligibleMember(frontier)) {
                dimensions.add(frontier.getDimension());
            }
        }
        for (CollectionOverlayKey key : overlaysByKey.keySet()) {
            if (key.collectionId().equals(collectionId)) {
                dimensions.add(key.dimension());
            }
        }

        for (ResourceKey<Level> dimension : dimensions) {
            refreshCollectionOverlay(collectionId, dimension);
        }
    }

    private List<FrontierOverlay> getEligibleMembers(UUID collectionId, ResourceKey<Level> dimension) {
        return collectionRuntime.getFrontiersInCollection(collectionId, dimension).stream()
                .filter(CollectionOverlayManager::isEligibleMember)
                .toList();
    }

    private static boolean isEligibleMember(FrontierOverlay frontier) {
        return frontier.getShape() != FrontierShape.Path;
    }

    private void registerOverlay(CollectionOverlay overlay) {
        overlaysByKey.put(overlay.getKey(), overlay);
        overlay.setDirtyOverlayListener(() -> markOverlayDirty(overlay));
    }

    private void deleteOverlay(CollectionOverlayKey key) {
        CollectionOverlay overlay = overlaysByKey.remove(key);
        if (overlay == null) {
            return;
        }

        dirtyOverlays.remove(overlay);
        overlay.deleted();
    }

    private void clearOverlays() {
        for (CollectionOverlay overlay : overlaysByKey.values()) {
            overlay.deleted();
        }

        overlaysByKey.clear();
        dirtyOverlays.clear();
    }

    private void markOverlayDirty(CollectionOverlay overlay) {
        if (overlaysByKey.get(overlay.getKey()) == overlay) {
            dirtyOverlays.add(overlay);
        }
    }
}
