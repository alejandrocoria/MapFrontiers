package games.alejandrocoria.mapfrontiers.common.identity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public final class PlayerNameEvents {
    private final Map<Object, Consumer<PlayerId>> changedSubscribers = new HashMap<>();

    public void subscribeChanged(Object owner, Consumer<PlayerId> callback) {
        changedSubscribers.put(owner, callback);
    }

    public void unsubscribe(Object owner) {
        changedSubscribers.remove(owner);
    }

    void postChanged(PlayerId playerId) {
        for (Consumer<PlayerId> callback : new ArrayList<>(changedSubscribers.values())) {
            callback.accept(playerId);
        }
    }

    public void close() {
        changedSubscribers.clear();
    }
}
