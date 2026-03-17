package games.alejandrocoria.mapfrontiers.api.plugin;

import games.alejandrocoria.mapfrontiers.api.server.IMapFrontiersServerAPI;

/**
 * Server-side extension point for integrating with MapFrontiers.
 */
public interface IMapFrontiersServerPlugin {
    /**
     * Stable plugin identifier. Must be unique among registered server plugins.
     *
     * @return unique plugin id
     */
    String getModId();

    /**
     * Called when the server API becomes available for the current world/session.
     *
     * @param api server API facade scoped to this plugin
     */
    void initialize(IMapFrontiersServerAPI api);

    /**
     * Called before the server API is cleared for the current world/session.
     *
     * @param api same API facade that was provided on initialize
     */
    void shutdown(IMapFrontiersServerAPI api);
}
