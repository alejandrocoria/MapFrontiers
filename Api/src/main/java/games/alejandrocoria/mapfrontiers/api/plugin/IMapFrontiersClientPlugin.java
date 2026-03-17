package games.alejandrocoria.mapfrontiers.api.plugin;

import games.alejandrocoria.mapfrontiers.api.client.IMapFrontiersClientAPI;

/**
 * Client-side extension point for integrating with MapFrontiers.
 */
public interface IMapFrontiersClientPlugin {
    /**
     * Stable plugin identifier. Must be unique among registered client plugins.
     *
     * @return unique plugin id
     */
    String getModId();

    /**
     * Called when the client API becomes available for the current world/session.
     *
     * @param api client API facade scoped to this plugin
     */
    void initialize(IMapFrontiersClientAPI api);

    /**
     * Called before the client API is cleared for the current world/session.
     *
     * @param api same API facade that was provided on initialize
     */
    void shutdown(IMapFrontiersClientAPI api);
}
