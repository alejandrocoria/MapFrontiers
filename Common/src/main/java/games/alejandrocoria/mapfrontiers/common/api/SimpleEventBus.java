package games.alejandrocoria.mapfrontiers.common.api;

import games.alejandrocoria.mapfrontiers.api.event.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SimpleEventBus implements EventBus {
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, key -> new ArrayList<>()).add((Consumer<Object>) listener);
    }

    @Override
    public synchronized void post(Object event) {
        List<Consumer<Object>> handlers = listeners.get(event.getClass());
        if (handlers == null) {
            return;
        }
        for (Consumer<Object> handler : handlers) {
            handler.accept(event);
        }
    }
}
