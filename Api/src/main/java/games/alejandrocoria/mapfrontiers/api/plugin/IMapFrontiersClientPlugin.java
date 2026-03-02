package games.alejandrocoria.mapfrontiers.api.plugin;

import games.alejandrocoria.mapfrontiers.api.client.IMapFrontiersClientAPI;

public interface IMapFrontiersClientPlugin {
    String getModId();
    void initialize(IMapFrontiersClientAPI api);
    void shutdown(IMapFrontiersClientAPI api);
}
