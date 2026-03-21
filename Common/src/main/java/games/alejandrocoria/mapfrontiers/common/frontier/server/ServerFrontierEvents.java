package games.alejandrocoria.mapfrontiers.common.frontier.server;

import games.alejandrocoria.mapfrontiers.common.FrontierData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ServerFrontierEvents {
    private final Map<Object, Consumer<FrontierData>> createdSubscribers = new HashMap<>();
    private final Map<Object, Consumer<FrontierData>> updatedSubscribers = new HashMap<>();
    private final Map<Object, Consumer<UUID>> deletedSubscribers = new HashMap<>();

    public void subscribeCreated(Object owner, Consumer<FrontierData> callback) {
        createdSubscribers.put(owner, callback);
    }

    public void subscribeUpdated(Object owner, Consumer<FrontierData> callback) {
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

    public void postCreated(FrontierData frontier) {
        for (Consumer<FrontierData> callback : createdSubscribers.values()) {
            callback.accept(frontier);
        }
    }

    public void postUpdated(FrontierData frontier) {
        for (Consumer<FrontierData> callback : updatedSubscribers.values()) {
            callback.accept(frontier);
        }
    }

    public void postDeleted(UUID frontierId) {
        for (Consumer<UUID> callback : deletedSubscribers.values()) {
            callback.accept(frontierId);
        }
    }

    public void close() {
        createdSubscribers.clear();
        updatedSubscribers.clear();
        deletedSubscribers.clear();
    }
}
