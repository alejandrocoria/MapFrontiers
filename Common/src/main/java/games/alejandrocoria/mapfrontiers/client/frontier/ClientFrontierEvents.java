package games.alejandrocoria.mapfrontiers.client.frontier;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ClientFrontierEvents {
    private final Map<Object, BiConsumer<FrontierOverlay, Integer>> createdSubscribers = new HashMap<>();
    private final Map<Object, BiConsumer<FrontierOverlay, Integer>> updatedSubscribers = new HashMap<>();
    private final Map<Object, Consumer<UUID>> deletedSubscribers = new HashMap<>();

    public void subscribeCreated(Object owner, BiConsumer<FrontierOverlay, Integer> callback) {
        createdSubscribers.put(owner, callback);
    }

    public void subscribeUpdated(Object owner, BiConsumer<FrontierOverlay, Integer> callback) {
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

    public void postCreated(FrontierOverlay frontierOverlay, int playerId) {
        for (BiConsumer<FrontierOverlay, Integer> callback : new ArrayList<>(createdSubscribers.values())) {
            callback.accept(frontierOverlay, playerId);
        }
    }

    public void postUpdated(FrontierOverlay frontierOverlay, int playerId) {
        for (BiConsumer<FrontierOverlay, Integer> callback : new ArrayList<>(updatedSubscribers.values())) {
            callback.accept(frontierOverlay, playerId);
        }
    }

    public void postDeleted(UUID frontierId) {
        for (Consumer<UUID> callback : new ArrayList<>(deletedSubscribers.values())) {
            callback.accept(frontierId);
        }
    }

    public void close() {
        createdSubscribers.clear();
        updatedSubscribers.clear();
        deletedSubscribers.clear();
    }
}
