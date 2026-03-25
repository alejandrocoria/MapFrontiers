package games.alejandrocoria.mapfrontiers.api.event;

import java.util.function.Consumer;

/**
 * Event stream for API-level frontier events.
 */
@SuppressWarnings("unused")
public interface EventBus {
    /**
     * Handle returned by {@link #subscribe(Class, Consumer)}.
     */
    interface Subscription {
        /**
         * Removes the listener from the event bus.
         */
        void unsubscribe();
    }

    /**
     * Subscribes a listener to a concrete event type.
     *
     * @param eventType event class to listen for
     * @param listener callback invoked for each event instance
     * @return subscription handle for manual unsubscribe
     */
    <T> Subscription subscribe(Class<T> eventType, Consumer<T> listener);
}
