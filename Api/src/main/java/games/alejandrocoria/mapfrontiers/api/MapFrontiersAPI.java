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

    public static void registerClientPlugin(IMapFrontiersClientPlugin plugin) {
        CLIENT_PLUGINS.add(plugin);
        if (clientAPI != null) {
            plugin.initialize(clientAPI);
        }
    }

    public static void registerServerPlugin(IMapFrontiersServerPlugin plugin) {
        SERVER_PLUGINS.add(plugin);
        if (serverAPI != null) {
            plugin.initialize(serverAPI);
        }
    }

    public static void setClientAPI(IMapFrontiersClientAPI api) {
        clientAPI = api;
        for (IMapFrontiersClientPlugin plugin : CLIENT_PLUGINS) {
            plugin.initialize(api);
        }
    }

    public static void setServerAPI(IMapFrontiersServerAPI api) {
        serverAPI = api;
        for (IMapFrontiersServerPlugin plugin : SERVER_PLUGINS) {
            plugin.initialize(api);
        }
    }

    public static void clearClientAPI() {
        clientAPI = null;
    }

    public static void clearServerAPI() {
        serverAPI = null;
    }
}
