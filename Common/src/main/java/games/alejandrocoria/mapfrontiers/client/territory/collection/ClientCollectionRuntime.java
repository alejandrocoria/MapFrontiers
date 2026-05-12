package games.alejandrocoria.mapfrontiers.client.territory.collection;

import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
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
        indexedFrontiersByCollectionId.clear();
        globalFrontiersWithoutCollection.clear();
        personalPersistentFrontiersWithoutCollection.clear();
        personalSessionFrontiersWithoutCollection.clear();
    }

    public void replaceCollections(List<CollectionData> globalCollections, List<CollectionData> personalCollections) {
        globalCollectionsById.clear();
        personalPersistentCollectionsById.clear();

        for (CollectionData collection : globalCollections) {
            globalCollectionsById.put(collection.getId(), new CollectionData(collection));
        }

        for (CollectionData collection : personalCollections) {
            if (!collection.isSessionOnly()) {
                personalPersistentCollectionsById.put(collection.getId(), new CollectionData(collection));
            }
        }
    }

    public void addOrUpdateCollection(CollectionData collection) {
        removeCollectionFromAllScopes(collection.getId());
        getCollectionMap(collection).put(collection.getId(), new CollectionData(collection));
    }

    public void deleteCollection(UUID collectionId) {
        removeCollectionFromAllScopes(collectionId);
    }

    public void refreshFromFrontiers(FrontiersOverlayManager globalManager, FrontiersOverlayManager personalManager) {
        indexedFrontiersByCollectionId.clear();
        globalFrontiersWithoutCollection.clear();
        personalPersistentFrontiersWithoutCollection.clear();
        personalSessionFrontiersWithoutCollection.clear();

        indexFrontiers(globalManager, globalFrontiersWithoutCollection);
        indexFrontiers(personalManager, personalPersistentFrontiersWithoutCollection, personalSessionFrontiersWithoutCollection);
        pruneNonOwnedPersonalCollectionsWithoutIndexedFrontiers();
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

    public List<CollectionData> getCollections(boolean personal) {
        if (!personal) {
            return getCollections(CollectionScope.GLOBAL_PERSISTENT);
        }

        List<CollectionData> collections = new ArrayList<>(personalPersistentCollectionsById.size()
                + personalSessionCollectionsById.size());
        collections.addAll(personalPersistentCollectionsById.values());
        collections.addAll(personalSessionCollectionsById.values());
        return List.copyOf(collections);
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

    public List<FrontierOverlay> getFrontiersWithoutCollection(boolean personal) {
        if (!personal) {
            return getFrontiersWithoutCollection(CollectionScope.GLOBAL_PERSISTENT);
        }

        List<FrontierOverlay> frontiers = new ArrayList<>(personalPersistentFrontiersWithoutCollection.size()
                + personalSessionFrontiersWithoutCollection.size());
        frontiers.addAll(personalPersistentFrontiersWithoutCollection);
        frontiers.addAll(personalSessionFrontiersWithoutCollection);
        return List.copyOf(frontiers);
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

    private void indexFrontiers(FrontiersOverlayManager manager, List<FrontierOverlay> frontiersWithoutCollection) {
        for (List<FrontierOverlay> frontiers : manager.getAllFrontiers().values()) {
            for (FrontierOverlay frontier : frontiers) {
                if (!frontier.hasCollection()) {
                    frontiersWithoutCollection.add(frontier);
                    continue;
                }

                indexedFrontiersByCollectionId.computeIfAbsent(frontier.getCollectionId(), ignored -> new ArrayList<>()).add(frontier);
            }
        }
    }

    private void indexFrontiers(FrontiersOverlayManager manager,
                                List<FrontierOverlay> persistentFrontiersWithoutCollection,
                                List<FrontierOverlay> sessionFrontiersWithoutCollection) {
        for (List<FrontierOverlay> frontiers : manager.getAllFrontiers().values()) {
            for (FrontierOverlay frontier : frontiers) {
                if (!frontier.hasCollection()) {
                    (frontier.isSessionOnly() ? sessionFrontiersWithoutCollection : persistentFrontiersWithoutCollection)
                            .add(frontier);
                    continue;
                }

                indexedFrontiersByCollectionId.computeIfAbsent(frontier.getCollectionId(), ignored -> new ArrayList<>()).add(frontier);
            }
        }
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

    private void removeCollectionFromAllScopes(UUID collectionId) {
        globalCollectionsById.remove(collectionId);
        personalPersistentCollectionsById.remove(collectionId);
        personalSessionCollectionsById.remove(collectionId);
    }
}
