package games.alejandrocoria.mapfrontiers.api.event;

import java.util.function.Consumer;

public interface EventBus {
    <T> void subscribe(Class<T> eventType, Consumer<T> listener);
    void post(Object event);
}
