package games.alejandrocoria.mapfrontiers.client.frontier;

import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
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
    private final Map<UUID, CollectionData> personalCollectionsById = new LinkedHashMap<>();
    private final Map<UUID, List<FrontierOverlay>> visibleFrontiersByCollectionId = new LinkedHashMap<>();
    private final List<FrontierOverlay> globalFrontiersWithoutCollection = new ArrayList<>();
    private final List<FrontierOverlay> personalFrontiersWithoutCollection = new ArrayList<>();

    public void clear() {
        globalCollectionsById.clear();
        personalCollectionsById.clear();
        visibleFrontiersByCollectionId.clear();
        globalFrontiersWithoutCollection.clear();
        personalFrontiersWithoutCollection.clear();
    }

    public void replaceCollections(List<CollectionData> globalCollections, List<CollectionData> personalCollections) {
        globalCollectionsById.clear();
        personalCollectionsById.clear();

        for (CollectionData collection : globalCollections) {
            globalCollectionsById.put(collection.getId(), new CollectionData(collection));
        }

        for (CollectionData collection : personalCollections) {
            personalCollectionsById.put(collection.getId(), new CollectionData(collection));
        }
    }

    public void addOrUpdateCollection(CollectionData collection) {
        Map<UUID, CollectionData> target = collection.getPersonal() ? personalCollectionsById : globalCollectionsById;
        Map<UUID, CollectionData> other = collection.getPersonal() ? globalCollectionsById : personalCollectionsById;

        other.remove(collection.getId());
        target.put(collection.getId(), new CollectionData(collection));
    }

    public void deleteCollection(UUID collectionId) {
        globalCollectionsById.remove(collectionId);
        personalCollectionsById.remove(collectionId);
    }

    public void refreshFromFrontiers(FrontiersOverlayManager globalManager, FrontiersOverlayManager personalManager) {
        visibleFrontiersByCollectionId.clear();
        globalFrontiersWithoutCollection.clear();
        personalFrontiersWithoutCollection.clear();

        indexFrontiers(globalManager, globalFrontiersWithoutCollection);
        indexFrontiers(personalManager, personalFrontiersWithoutCollection);
        pruneIndirectPersonalCollectionsWithoutVisibleFrontiers();
    }

    public @Nullable CollectionData getCollection(UUID collectionId) {
        CollectionData collection = personalCollectionsById.get(collectionId);
        if (collection != null) {
            return collection;
        }

        return globalCollectionsById.get(collectionId);
    }

    public List<CollectionData> getCollections(boolean personal) {
        return List.copyOf((personal ? personalCollectionsById : globalCollectionsById).values());
    }

    public boolean hasCollection(UUID collectionId) {
        return personalCollectionsById.containsKey(collectionId) || globalCollectionsById.containsKey(collectionId);
    }

    public @Nullable CollectionData getPersonalCollectionCopiedFrom(UUID copiedFromId) {
        for (CollectionData collection : personalCollectionsById.values()) {
            if (collection.wasCopied() && collection.getCopiedFromId().equals(copiedFromId)) {
                return collection;
            }
        }

        return null;
    }

    public List<FrontierOverlay> getFrontiersInCollection(UUID collectionId) {
        List<FrontierOverlay> frontiers = visibleFrontiersByCollectionId.get(collectionId);
        return frontiers == null ? List.of() : List.copyOf(frontiers);
    }

    public List<FrontierOverlay> getFrontiersWithoutCollection(boolean personal) {
        return personal ? List.copyOf(personalFrontiersWithoutCollection) : List.copyOf(globalFrontiersWithoutCollection);
    }

    public int getVisibleFrontierCount(UUID collectionId) {
        List<FrontierOverlay> frontiers = visibleFrontiersByCollectionId.get(collectionId);
        return frontiers == null ? 0 : frontiers.size();
    }

    private void indexFrontiers(FrontiersOverlayManager manager, List<FrontierOverlay> frontiersWithoutCollection) {
        for (List<FrontierOverlay> frontiers : manager.getAllFrontiers().values()) {
            for (FrontierOverlay frontier : frontiers) {
                if (!frontier.hasCollection()) {
                    frontiersWithoutCollection.add(frontier);
                    continue;
                }

                visibleFrontiersByCollectionId.computeIfAbsent(frontier.getCollectionId(), ignored -> new ArrayList<>()).add(frontier);
            }
        }
    }

    private void pruneIndirectPersonalCollectionsWithoutVisibleFrontiers() {
        SettingsUser currentPlayer = mc.player == null ? null : new SettingsUser(mc.player);
        if (currentPlayer == null) {
            return;
        }

        personalCollectionsById.entrySet().removeIf(entry -> !entry.getValue().getOwner().equals(currentPlayer)
                && getVisibleFrontierCount(entry.getKey()) == 0);
    }
}
