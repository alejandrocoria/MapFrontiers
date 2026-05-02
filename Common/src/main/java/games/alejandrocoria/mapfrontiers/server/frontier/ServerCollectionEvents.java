package games.alejandrocoria.mapfrontiers.server.frontier;

import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ServerCollectionEvents {
    private final Map<Object, Consumer<CollectionData>> createdSubscribers = new HashMap<>();
    private final Map<Object, Consumer<CollectionData>> updatedSubscribers = new HashMap<>();
    private final Map<Object, Consumer<CollectionData>> deletedSubscribers = new HashMap<>();

    public void subscribeCreated(Object owner, Consumer<CollectionData> callback) {
        createdSubscribers.put(owner, callback);
    }

    public void subscribeUpdated(Object owner, Consumer<CollectionData> callback) {
        updatedSubscribers.put(owner, callback);
    }

    public void subscribeDeleted(Object owner, Consumer<CollectionData> callback) {
        deletedSubscribers.put(owner, callback);
    }

    public void unsubscribe(Object owner) {
        createdSubscribers.remove(owner);
        updatedSubscribers.remove(owner);
        deletedSubscribers.remove(owner);
    }

    public void postCreated(CollectionData collection) {
        for (Consumer<CollectionData> callback : createdSubscribers.values()) {
            callback.accept(collection);
        }
    }

    public void postUpdated(CollectionData collection) {
        for (Consumer<CollectionData> callback : updatedSubscribers.values()) {
            callback.accept(collection);
        }
    }

    public void postDeleted(CollectionData collection) {
        for (Consumer<CollectionData> callback : deletedSubscribers.values()) {
            callback.accept(collection);
        }
    }

    public void close() {
        createdSubscribers.clear();
        updatedSubscribers.clear();
        deletedSubscribers.clear();
    }
}
