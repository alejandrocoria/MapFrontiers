package games.alejandrocoria.mapfrontiers.api;

import games.alejandrocoria.mapfrontiers.api.client.ClientFrontierService;
import games.alejandrocoria.mapfrontiers.api.client.IMapFrontiersClientAPI;
import games.alejandrocoria.mapfrontiers.api.event.EventBus;
import games.alejandrocoria.mapfrontiers.api.plugin.IMapFrontiersClientPlugin;
import games.alejandrocoria.mapfrontiers.api.plugin.IMapFrontiersServerPlugin;
import games.alejandrocoria.mapfrontiers.api.server.IMapFrontiersServerAPI;
import games.alejandrocoria.mapfrontiers.api.server.ServerFrontierService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static API entry point used by plugins to register and obtain client/server API instances.
 */
public final class MapFrontiersAPI {
    private static final Logger LOGGER = Logger.getLogger(MapFrontiersAPI.class.getName());

    private static final List<IMapFrontiersClientPlugin> CLIENT_PLUGINS = new ArrayList<>();
    private static final List<IMapFrontiersServerPlugin> SERVER_PLUGINS = new ArrayList<>();
    private static final Map<IMapFrontiersClientPlugin, PluginClientAPI> CLIENT_PLUGIN_APIS = new HashMap<>();
    private static final Map<IMapFrontiersServerPlugin, PluginServerAPI> SERVER_PLUGIN_APIS = new HashMap<>();

    private static IMapFrontiersClientAPI clientAPI;
    private static IMapFrontiersServerAPI serverAPI;

    private MapFrontiersAPI() {
    }

    /**
     * Returns the currently active server API, if any.
     *
     * @return server API when available
     */
    public static Optional<IMapFrontiersServerAPI> getServerAPI() {
        return Optional.ofNullable(serverAPI);
    }

    /**
     * Returns the currently active client API, if any.
     *
     * @return client API when available
     */
    public static Optional<IMapFrontiersClientAPI> getClientAPI() {
        return Optional.ofNullable(clientAPI);
    }

    /**
     * Registers a client plugin.
     * If client API is already active, plugin initialization runs immediately.
     * Duplicate mod ids are ignored.
     *
     * @param plugin plugin instance to register
     */
    public static synchronized void registerClientPlugin(IMapFrontiersClientPlugin plugin) {
        if (CLIENT_PLUGINS.stream().anyMatch(p -> p.getModId().equals(plugin.getModId()))) {
            return;
        }
        CLIENT_PLUGINS.add(plugin);
        if (clientAPI != null) {
            initializeClientPlugin(plugin);
        }
    }

    /**
     * Registers a server plugin.
     * If server API is already active, plugin initialization runs immediately.
     * Duplicate mod ids are ignored.
     *
     * @param plugin plugin instance to register
     */
    public static synchronized void registerServerPlugin(IMapFrontiersServerPlugin plugin) {
        if (SERVER_PLUGINS.stream().anyMatch(p -> p.getModId().equals(plugin.getModId()))) {
            return;
        }
        SERVER_PLUGINS.add(plugin);
        if (serverAPI != null) {
            initializeServerPlugin(plugin);
        }
    }

    static synchronized void setClientAPI(IMapFrontiersClientAPI api) {
        clientAPI = api;
        CLIENT_PLUGIN_APIS.clear();
        for (IMapFrontiersClientPlugin plugin : CLIENT_PLUGINS) {
            initializeClientPlugin(plugin);
        }
    }

    static synchronized void setServerAPI(IMapFrontiersServerAPI api) {
        serverAPI = api;
        SERVER_PLUGIN_APIS.clear();
        for (IMapFrontiersServerPlugin plugin : SERVER_PLUGINS) {
            initializeServerPlugin(plugin);
        }
    }

    static synchronized void clearClientAPI() {
        if (clientAPI == null) {
            return;
        }

        for (IMapFrontiersClientPlugin plugin : CLIENT_PLUGINS) {
            PluginClientAPI api = CLIENT_PLUGIN_APIS.get(plugin);
            if (api == null) {
                continue;
            }
            api.clearSubscriptions();
            try {
                plugin.shutdown(api);
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Client plugin shutdown failed: " + plugin.getModId(), t);
            }
        }

        CLIENT_PLUGIN_APIS.clear();
        clientAPI = null;
    }

    static synchronized void clearServerAPI() {
        if (serverAPI == null) {
            return;
        }

        for (IMapFrontiersServerPlugin plugin : SERVER_PLUGINS) {
            PluginServerAPI api = SERVER_PLUGIN_APIS.get(plugin);
            if (api == null) {
                continue;
            }
            api.clearSubscriptions();
            try {
                plugin.shutdown(api);
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Server plugin shutdown failed: " + plugin.getModId(), t);
            }
        }

        SERVER_PLUGIN_APIS.clear();
        serverAPI = null;
    }

    private static void initializeClientPlugin(IMapFrontiersClientPlugin plugin) {
        if (clientAPI == null) {
            return;
        }

        PluginClientAPI api = new PluginClientAPI(clientAPI);
        CLIENT_PLUGIN_APIS.put(plugin, api);
        try {
            plugin.initialize(api);
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Client plugin initialize failed: " + plugin.getModId(), t);
        }
    }

    private static void initializeServerPlugin(IMapFrontiersServerPlugin plugin) {
        if (serverAPI == null) {
            return;
        }

        PluginServerAPI api = new PluginServerAPI(serverAPI);
        SERVER_PLUGIN_APIS.put(plugin, api);
        try {
            plugin.initialize(api);
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Server plugin initialize failed: " + plugin.getModId(), t);
        }
    }

    private static final class TrackingEventBus implements EventBus {
        private final EventBus delegate;
        private final List<Subscription> subscriptions = new ArrayList<>();

        private TrackingEventBus(EventBus delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized <T> Subscription subscribe(Class<T> eventType, Consumer<T> listener) {
            Subscription subscription = delegate.subscribe(eventType, listener);
            subscriptions.add(subscription);
            return () -> {
                synchronized (TrackingEventBus.this) {
                    subscriptions.remove(subscription);
                }
                subscription.unsubscribe();
            };
        }

        private synchronized void clearSubscriptions() {
            List<Subscription> snapshot = List.copyOf(subscriptions);
            subscriptions.clear();
            for (Subscription subscription : snapshot) {
                subscription.unsubscribe();
            }
        }
    }

    private static final class PluginClientAPI implements IMapFrontiersClientAPI {
        private final IMapFrontiersClientAPI delegate;
        private final TrackingEventBus events;

        private PluginClientAPI(IMapFrontiersClientAPI delegate) {
            this.delegate = delegate;
            this.events = new TrackingEventBus(delegate.events());
        }

        @Override
        public ClientFrontierService frontiers() {
            return delegate.frontiers();
        }

        @Override
        public EventBus events() {
            return events;
        }

        private void clearSubscriptions() {
            events.clearSubscriptions();
        }
    }

    private static final class PluginServerAPI implements IMapFrontiersServerAPI {
        private final IMapFrontiersServerAPI delegate;
        private final TrackingEventBus events;

        private PluginServerAPI(IMapFrontiersServerAPI delegate) {
            this.delegate = delegate;
            this.events = new TrackingEventBus(delegate.events());
        }

        @Override
        public ServerFrontierService frontiers() {
            return delegate.frontiers();
        }

        @Override
        public EventBus events() {
            return events;
        }

        private void clearSubscriptions() {
            events.clearSubscriptions();
        }
    }
}
