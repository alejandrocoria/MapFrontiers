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
    public synchronized <T> Subscription subscribe(Class<T> eventType, Consumer<T> listener) {
        Consumer<Object> handler = (Consumer<Object>) listener;
        listeners.computeIfAbsent(eventType, key -> new ArrayList<>()).add(handler);

        return () -> unsubscribe(eventType, handler);
    }

    private synchronized void unsubscribe(Class<?> eventType, Consumer<Object> listener) {
        List<Consumer<Object>> handlers = listeners.get(eventType);
        if (handlers == null) {
            return;
        }

        handlers.remove(listener);
        if (handlers.isEmpty()) {
            listeners.remove(eventType);
        }
    }

    public synchronized void post(Object event) {
        List<Consumer<Object>> handlers = listeners.get(event.getClass());
        if (handlers == null) {
            return;
        }

        // Snapshot to allow listeners to unsubscribe safely during dispatch.
        List<Consumer<Object>> snapshot = List.copyOf(handlers);
        for (Consumer<Object> handler : snapshot) {
            handler.accept(event);
        }
    }
}
