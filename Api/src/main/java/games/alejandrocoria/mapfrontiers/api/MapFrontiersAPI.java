package games.alejandrocoria.mapfrontiers.api;

import games.alejandrocoria.mapfrontiers.api.client.ClientFrontierService;
import games.alejandrocoria.mapfrontiers.api.client.FrontierActionResult;
import games.alejandrocoria.mapfrontiers.api.client.IMapFrontiersClientAPI;
import games.alejandrocoria.mapfrontiers.api.event.EventBus;
import games.alejandrocoria.mapfrontiers.api.internal.InternalMapFrontiersClientAPI;
import games.alejandrocoria.mapfrontiers.api.internal.InternalMapFrontiersServerAPI;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedClientFrontierService;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.FrontierSharePermission;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.api.plugin.IMapFrontiersClientPlugin;
import games.alejandrocoria.mapfrontiers.api.plugin.IMapFrontiersServerPlugin;
import games.alejandrocoria.mapfrontiers.api.server.IMapFrontiersServerAPI;
import games.alejandrocoria.mapfrontiers.api.server.ServerFrontierService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static API entry point used by plugins to register client and server integrations.
 */
@SuppressWarnings("unused")
public final class MapFrontiersAPI {
    private static final Logger LOGGER = Logger.getLogger(MapFrontiersAPI.class.getName());

    private static final List<IMapFrontiersClientPlugin> CLIENT_PLUGINS = new ArrayList<>();
    private static final List<IMapFrontiersServerPlugin> SERVER_PLUGINS = new ArrayList<>();
    private static final Map<IMapFrontiersClientPlugin, PluginClientAPI> CLIENT_PLUGIN_APIS = new HashMap<>();
    private static final Map<IMapFrontiersServerPlugin, PluginServerAPI> SERVER_PLUGIN_APIS = new HashMap<>();

    private static InternalMapFrontiersClientAPI clientAPI;
    private static InternalMapFrontiersServerAPI serverAPI;

    private MapFrontiersAPI() {
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

    static synchronized void setClientAPI(InternalMapFrontiersClientAPI api) {
        clientAPI = api;
        CLIENT_PLUGIN_APIS.clear();
        for (IMapFrontiersClientPlugin plugin : CLIENT_PLUGINS) {
            initializeClientPlugin(plugin);
        }
    }

    static synchronized void setServerAPI(InternalMapFrontiersServerAPI api) {
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

        PluginClientAPI api = new PluginClientAPI(clientAPI, plugin.getModId());
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

        PluginServerAPI api = new PluginServerAPI(serverAPI, plugin.getModId());
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
        private final ClientFrontierService frontiers;
        private final TrackingEventBus events;

        private PluginClientAPI(InternalMapFrontiersClientAPI delegate, String pluginModId) {
            this.frontiers = new PluginClientFrontierService(delegate.frontiers(), pluginModId);
            this.events = new TrackingEventBus(delegate.events());
        }

        @Override
        public ClientFrontierService frontiers() {
            return frontiers;
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
        private final ServerFrontierService frontiers;
        private final TrackingEventBus events;

        private PluginServerAPI(InternalMapFrontiersServerAPI delegate, String pluginModId) {
            this.frontiers = new PluginServerFrontierService(delegate.frontiers(), pluginModId);
            this.events = new TrackingEventBus(delegate.events());
        }

        @Override
        public ServerFrontierService frontiers() {
            return frontiers;
        }

        @Override
        public EventBus events() {
            return events;
        }

        private void clearSubscriptions() {
            events.clearSubscriptions();
        }
    }

    private static final class PluginClientFrontierService implements ClientFrontierService {
        private final PluginScopedClientFrontierService delegate;
        private final String pluginModId;

        private PluginClientFrontierService(PluginScopedClientFrontierService delegate, String pluginModId) {
            this.delegate = delegate;
            this.pluginModId = pluginModId;
        }

        @Override
        public Optional<FrontierDataView> getFrontier(FrontierId frontierId) {
            return delegate.getFrontier(pluginModId, frontierId);
        }

        @Override
        public FrontierActionResult createGlobalFrontier(DimensionId dimension, FrontierShape shape) {
            return delegate.createGlobalFrontier(pluginModId, dimension, shape);
        }

        @Override
        public FrontierActionResult updateGlobalFrontier(FrontierId frontierId, FrontierMutation mutation) {
            return delegate.updateGlobalFrontier(pluginModId, frontierId, mutation);
        }

        @Override
        public FrontierActionResult deleteGlobalFrontier(FrontierId frontierId) {
            return delegate.deleteGlobalFrontier(pluginModId, frontierId);
        }

        @Override
        public FrontierActionResult changeToPersonal(FrontierId frontierId) {
            return delegate.changeToPersonal(pluginModId, frontierId);
        }

        @Override
        public List<FrontierDataView> listGlobalFrontiers(DimensionId dimension) {
            return delegate.listGlobalFrontiers(pluginModId, dimension);
        }

        @Override
        public FrontierActionResult createPersonalFrontier(DimensionId dimension, FrontierShape shape) {
            return delegate.createPersonalFrontier(pluginModId, dimension, shape);
        }

        @Override
        public FrontierActionResult updatePersonalFrontier(FrontierId frontierId, FrontierMutation mutation) {
            return delegate.updatePersonalFrontier(pluginModId, frontierId, mutation);
        }

        @Override
        public FrontierActionResult deletePersonalFrontier(FrontierId frontierId) {
            return delegate.deletePersonalFrontier(pluginModId, frontierId);
        }

        @Override
        public FrontierActionResult changeToGlobal(FrontierId frontierId) {
            return delegate.changeToGlobal(pluginModId, frontierId);
        }

        @Override
        public List<FrontierDataView> listPersonalFrontiers(DimensionId dimension) {
            return delegate.listPersonalFrontiers(pluginModId, dimension);
        }

        @Override
        public FrontierActionResult sharePersonalFrontier(FrontierId frontierId, UserRef user, Set<FrontierSharePermission> permissions) {
            return delegate.sharePersonalFrontier(pluginModId, frontierId, user, permissions);
        }

        @Override
        public FrontierActionResult updateSharedUserPermissions(FrontierId frontierId,
                                                                UserRef user,
                                                                Set<FrontierSharePermission> permissions) {
            return delegate.updateSharedUserPermissions(pluginModId, frontierId, user, permissions);
        }

        @Override
        public FrontierActionResult updateSharedUserPermissions(FrontierId frontierId,
                                                                UserRef user,
                                                                Set<FrontierSharePermission> permissionsToAdd,
                                                                Set<FrontierSharePermission> permissionsToRemove) {
            return delegate.updateSharedUserPermissions(pluginModId, frontierId, user, permissionsToAdd, permissionsToRemove);
        }

        @Override
        public FrontierActionResult removeSharedUser(FrontierId frontierId, UserRef user) {
            return delegate.removeSharedUser(pluginModId, frontierId, user);
        }
    }

    private static final class PluginServerFrontierService implements ServerFrontierService {
        private final PluginScopedServerFrontierService delegate;
        private final String pluginModId;

        private PluginServerFrontierService(PluginScopedServerFrontierService delegate, String pluginModId) {
            this.delegate = delegate;
            this.pluginModId = pluginModId;
        }

        @Override
        public FrontierDataView createGlobalFrontier(UserRef owner, DimensionId dimension, FrontierShape shape) {
            return delegate.createGlobalFrontier(pluginModId, owner, dimension, shape);
        }

        @Override
        public Optional<FrontierDataView> updateGlobalFrontier(FrontierId frontierId, FrontierMutation mutation) {
            return delegate.updateGlobalFrontier(pluginModId, frontierId, mutation);
        }

        @Override
        public boolean deleteGlobalFrontier(FrontierId frontierId) {
            return delegate.deleteGlobalFrontier(pluginModId, frontierId);
        }

        @Override
        public List<FrontierDataView> listGlobalFrontiers(DimensionId dimension) {
            return delegate.listGlobalFrontiers(pluginModId, dimension);
        }

        @Override
        public Optional<FrontierDataView> getFrontier(FrontierId frontierId) {
            return delegate.getFrontier(pluginModId, frontierId);
        }
    }
}
