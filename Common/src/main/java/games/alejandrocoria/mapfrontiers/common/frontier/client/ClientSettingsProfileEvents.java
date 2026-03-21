package games.alejandrocoria.mapfrontiers.common.frontier.client;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ClientSettingsProfileEvents {
    private final Map<Object, Consumer<SettingsProfile>> updatedSubscribers = new HashMap<>();

    public void subscribeUpdated(Object owner, Consumer<SettingsProfile> callback) {
        updatedSubscribers.put(owner, callback);
    }

    public void unsubscribe(Object owner) {
        updatedSubscribers.remove(owner);
    }

    public void postUpdated(SettingsProfile profile) {
        for (Consumer<SettingsProfile> callback : updatedSubscribers.values()) {
            callback.accept(profile);
        }
    }

    public void close() {
        updatedSubscribers.clear();
    }
}
