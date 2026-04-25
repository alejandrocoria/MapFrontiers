package games.alejandrocoria.mapfrontiers.client.frontier;

import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ClientCollectionEvents {
    private final Map<Object, Consumer<CollectionData>> createdSubscribers = new HashMap<>();
    private final Map<Object, Consumer<CollectionData>> updatedSubscribers = new HashMap<>();
    private final Map<Object, Consumer<UUID>> deletedSubscribers = new HashMap<>();

    public void subscribeCreated(Object owner, Consumer<CollectionData> callback) {
        createdSubscribers.put(owner, callback);
    }

    public void subscribeUpdated(Object owner, Consumer<CollectionData> callback) {
        updatedSubscribers.put(owner, callback);
    }

    public void subscribeDeleted(Object owner, Consumer<UUID> callback) {
        deletedSubscribers.put(owner, callback);
    }

    public void unsubscribe(Object owner) {
        createdSubscribers.remove(owner);
        updatedSubscribers.remove(owner);
        deletedSubscribers.remove(owner);
    }

    public void postCreated(CollectionData collection) {
        CollectionData snapshot = new CollectionData(collection);
        for (Consumer<CollectionData> callback : new ArrayList<>(createdSubscribers.values())) {
            callback.accept(snapshot);
        }
    }

    public void postUpdated(CollectionData collection) {
        CollectionData snapshot = new CollectionData(collection);
        for (Consumer<CollectionData> callback : new ArrayList<>(updatedSubscribers.values())) {
            callback.accept(snapshot);
        }
    }

    public void postDeleted(UUID collectionId) {
        for (Consumer<UUID> callback : new ArrayList<>(deletedSubscribers.values())) {
            callback.accept(collectionId);
        }
    }

    public void close() {
        createdSubscribers.clear();
        updatedSubscribers.clear();
        deletedSubscribers.clear();
    }
}
