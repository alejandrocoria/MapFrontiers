package games.alejandrocoria.mapfrontiers.api.plugin;

import games.alejandrocoria.mapfrontiers.api.server.IMapFrontiersServerAPI;

public interface IMapFrontiersServerPlugin {
    String getModId();
    void initialize(IMapFrontiersServerAPI api);
}
