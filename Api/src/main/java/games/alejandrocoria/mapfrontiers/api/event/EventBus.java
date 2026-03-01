package games.alejandrocoria.mapfrontiers.api.event;

import java.util.function.Consumer;

public interface EventBus {
    interface Subscription {
        void unsubscribe();
    }

    <T> Subscription subscribe(Class<T> eventType, Consumer<T> listener);
}
