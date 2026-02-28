package games.alejandrocoria.mapfrontiers.api;

import games.alejandrocoria.mapfrontiers.api.client.IMapFrontiersClientAPI;
import games.alejandrocoria.mapfrontiers.api.plugin.IMapFrontiersClientPlugin;
import games.alejandrocoria.mapfrontiers.api.plugin.IMapFrontiersServerPlugin;
import games.alejandrocoria.mapfrontiers.api.server.IMapFrontiersServerAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MapFrontiersAPI {
    private static final List<IMapFrontiersClientPlugin> CLIENT_PLUGINS = new ArrayList<>();
    private static final List<IMapFrontiersServerPlugin> SERVER_PLUGINS = new ArrayList<>();

    private static IMapFrontiersClientAPI clientAPI;
    private static IMapFrontiersServerAPI serverAPI;

    private MapFrontiersAPI() {
    }

    public static Optional<IMapFrontiersServerAPI> getServerAPI() {
        return Optional.ofNullable(serverAPI);
    }

    public static Optional<IMapFrontiersClientAPI> getClientAPI() {
        return Optional.ofNullable(clientAPI);
    }

    public static synchronized void registerClientPlugin(IMapFrontiersClientPlugin plugin) {
        if (CLIENT_PLUGINS.stream().anyMatch(p -> p.getModId().equals(plugin.getModId()))) {
            return;
        }
        CLIENT_PLUGINS.add(plugin);
        if (clientAPI != null) {
            plugin.initialize(clientAPI);
        }
    }

    public static synchronized void registerServerPlugin(IMapFrontiersServerPlugin plugin) {
        if (SERVER_PLUGINS.stream().anyMatch(p -> p.getModId().equals(plugin.getModId()))) {
            return;
        }
        SERVER_PLUGINS.add(plugin);
        if (serverAPI != null) {
            plugin.initialize(serverAPI);
        }
    }

    public static synchronized void setClientAPI(IMapFrontiersClientAPI api) {
        clientAPI = api;
        for (IMapFrontiersClientPlugin plugin : CLIENT_PLUGINS) {
            plugin.initialize(api);
        }
    }

    public static synchronized void setServerAPI(IMapFrontiersServerAPI api) {
        serverAPI = api;
        for (IMapFrontiersServerPlugin plugin : SERVER_PLUGINS) {
            plugin.initialize(api);
        }
    }

    public static synchronized void clearClientAPI() {
        clientAPI = null;
    }

    public static synchronized void clearServerAPI() {
        serverAPI = null;
    }
}
