package games.alejandrocoria.mapfrontiers.client.territory.collection;

import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierData;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ClientCollectionRuntime {
    private static final Minecraft mc = Minecraft.getInstance();

    private final Map<UUID, CollectionData> globalCollectionsById = new LinkedHashMap<>();
    private final Map<UUID, CollectionData> personalPersistentCollectionsById = new LinkedHashMap<>();
    private final Map<UUID, CollectionData> personalSessionCollectionsById = new LinkedHashMap<>();
    private final Map<UUID, List<FrontierOverlay>> indexedFrontiersByCollectionId = new LinkedHashMap<>();
    private final List<FrontierOverlay> globalFrontiersWithoutCollection = new ArrayList<>();
    private final List<FrontierOverlay> personalPersistentFrontiersWithoutCollection = new ArrayList<>();
    private final List<FrontierOverlay> personalSessionFrontiersWithoutCollection = new ArrayList<>();

    public void clear() {
        globalCollectionsById.clear();
        personalPersistentCollectionsById.clear();
        personalSessionCollectionsById.clear();
        clearFrontierIndexes();
    }

    public void clearFrontierIndexes() {
        indexedFrontiersByCollectionId.clear();
        globalFrontiersWithoutCollection.clear();
        personalPersistentFrontiersWithoutCollection.clear();
        personalSessionFrontiersWithoutCollection.clear();
    }

    public void replaceCollections(List<CollectionData> globalCollections, List<CollectionData> personalCollections) {
        globalCollectionsById.clear();
        personalPersistentCollectionsById.clear();
        personalSessionCollectionsById.clear();

        for (CollectionData collection : globalCollections) {
            onCollectionUpserted(collection);
        }

        for (CollectionData collection : personalCollections) {
            if (!collection.isSessionOnly()) {
                onCollectionUpserted(collection);
            }
        }
    }

    public void onCollectionUpserted(CollectionData collection) {
        removeCollectionFromAllScopes(collection.getId());
        getCollectionMap(collection).put(collection.getId(), new CollectionData(collection));
    }

    public void onCollectionDeleted(UUID collectionId) {
        removeCollectionFromAllScopes(collectionId);
    }

    public void replaceFrontierIndexes(FrontiersOverlayManager globalManager, FrontiersOverlayManager personalManager) {
        clearFrontierIndexes();
        indexFrontiers(globalManager);
        indexFrontiers(personalManager);
        pruneNonOwnedPersonalCollectionsWithoutIndexedFrontiers();
    }

    public void onFrontierAdded(FrontierOverlay frontier) {
        addFrontierToIndex(frontier, snapshotFrontier(frontier));
    }

    public void onFrontierRemoved(FrontierOverlay frontier) {
        removeFrontierFromIndex(frontier, snapshotFrontier(frontier));
        pruneNonOwnedPersonalCollectionsWithoutIndexedFrontiers();
    }

    public void onFrontierUpdated(FrontierIndexState previousState, FrontierOverlay frontier) {
        FrontierIndexState currentState = snapshotFrontier(frontier);
        if (previousState.equals(currentState)) {
            return;
        }

        removeFrontierFromIndex(frontier, previousState);
        addFrontierToIndex(frontier, currentState);
        pruneNonOwnedPersonalCollectionsWithoutIndexedFrontiers();
    }

    public FrontierIndexState snapshotFrontier(FrontierData frontier) {
        return new FrontierIndexState(frontier.getCollectionId(), getFrontierScope(frontier));
    }

    public @Nullable CollectionData getCollection(UUID collectionId) {
        CollectionData collection = personalPersistentCollectionsById.get(collectionId);
        if (collection != null) {
            return collection;
        }

        collection = personalSessionCollectionsById.get(collectionId);
        if (collection != null) {
            return collection;
        }

        return globalCollectionsById.get(collectionId);
    }

    public List<CollectionData> getCollections(CollectionScope scope) {
        return switch (scope) {
            case GLOBAL_PERSISTENT -> List.copyOf(globalCollectionsById.values());
            case PERSONAL_PERSISTENT -> List.copyOf(personalPersistentCollectionsById.values());
            case PERSONAL_SESSION -> List.copyOf(personalSessionCollectionsById.values());
        };
    }

    public boolean hasCollection(UUID collectionId) {
        return globalCollectionsById.containsKey(collectionId)
                || personalPersistentCollectionsById.containsKey(collectionId)
                || personalSessionCollectionsById.containsKey(collectionId);
    }

    public @Nullable CollectionData getPersonalCollectionCopiedFrom(UUID copiedFromId) {
        for (CollectionData collection : personalPersistentCollectionsById.values()) {
            if (collection.wasCopied() && collection.getCopiedFromId().equals(copiedFromId)) {
                return collection;
            }
        }

        return null;
    }

    public List<FrontierOverlay> getFrontiersInCollection(UUID collectionId) {
        List<FrontierOverlay> frontiers = indexedFrontiersByCollectionId.get(collectionId);
        return frontiers == null ? List.of() : List.copyOf(frontiers);
    }

    public List<FrontierOverlay> getFrontiersWithoutCollection(CollectionScope scope) {
        return switch (scope) {
            case GLOBAL_PERSISTENT -> List.copyOf(globalFrontiersWithoutCollection);
            case PERSONAL_PERSISTENT -> List.copyOf(personalPersistentFrontiersWithoutCollection);
            case PERSONAL_SESSION -> List.copyOf(personalSessionFrontiersWithoutCollection);
        };
    }

    public int getIndexedFrontierCount(UUID collectionId) {
        List<FrontierOverlay> frontiers = indexedFrontiersByCollectionId.get(collectionId);
        return frontiers == null ? 0 : frontiers.size();
    }

    private void indexFrontiers(FrontiersOverlayManager manager) {
        for (List<FrontierOverlay> frontiers : manager.getAllFrontiers().values()) {
            for (FrontierOverlay frontier : frontiers) {
                onFrontierAdded(frontier);
            }
        }
    }

    private void addFrontierToIndex(FrontierOverlay frontier, FrontierIndexState state) {
        if (state.collectionId() != null) {
            indexedFrontiersByCollectionId.computeIfAbsent(state.collectionId(), ignored -> new ArrayList<>()).add(frontier);
            return;
        }

        getFrontiersWithoutCollectionList(state.scope()).add(frontier);
    }

    private void removeFrontierFromIndex(FrontierOverlay frontier, FrontierIndexState state) {
        if (state.collectionId() != null) {
            List<FrontierOverlay> frontiers = indexedFrontiersByCollectionId.get(state.collectionId());
            if (frontiers == null) {
                return;
            }

            frontiers.remove(frontier);
            if (frontiers.isEmpty()) {
                indexedFrontiersByCollectionId.remove(state.collectionId());
            }
            return;
        }

        getFrontiersWithoutCollectionList(state.scope()).remove(frontier);
    }

    private void pruneNonOwnedPersonalCollectionsWithoutIndexedFrontiers() {
        SettingsUser currentPlayer = mc.player == null ? null : new SettingsUser(mc.player);
        if (currentPlayer == null) {
            return;
        }

        personalPersistentCollectionsById.entrySet().removeIf(entry -> !entry.getValue().getOwner().equals(currentPlayer)
                && getIndexedFrontierCount(entry.getKey()) == 0);
    }

    private Map<UUID, CollectionData> getCollectionMap(CollectionData collection) {
        if (!collection.getPersonal()) {
            return globalCollectionsById;
        }

        return collection.isSessionOnly() ? personalSessionCollectionsById : personalPersistentCollectionsById;
    }

    private CollectionScope getFrontierScope(FrontierData frontier) {
        if (!frontier.getPersonal()) {
            return CollectionScope.GLOBAL_PERSISTENT;
        }

        return frontier.isSessionOnly() ? CollectionScope.PERSONAL_SESSION : CollectionScope.PERSONAL_PERSISTENT;
    }

    private List<FrontierOverlay> getFrontiersWithoutCollectionList(CollectionScope scope) {
        return switch (scope) {
            case GLOBAL_PERSISTENT -> globalFrontiersWithoutCollection;
            case PERSONAL_PERSISTENT -> personalPersistentFrontiersWithoutCollection;
            case PERSONAL_SESSION -> personalSessionFrontiersWithoutCollection;
        };
    }

    private void removeCollectionFromAllScopes(UUID collectionId) {
        globalCollectionsById.remove(collectionId);
        personalPersistentCollectionsById.remove(collectionId);
        personalSessionCollectionsById.remove(collectionId);
    }

    public record FrontierIndexState(@Nullable UUID collectionId, CollectionScope scope) {
    }
}
