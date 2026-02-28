package games.alejandrocoria.mapfrontiers.common.api.server;

import games.alejandrocoria.mapfrontiers.api.event.EventBus;
import games.alejandrocoria.mapfrontiers.api.server.IMapFrontiersServerAPI;
import games.alejandrocoria.mapfrontiers.api.server.ServerFrontierService;
import games.alejandrocoria.mapfrontiers.api.server.ServerPermissionService;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;

public class MapFrontiersServerAPIImpl implements IMapFrontiersServerAPI {
    private final ServerFrontierService frontiers;
    private final ServerPermissionService permissions;
    private final SimpleEventBus eventBus;

    public MapFrontiersServerAPIImpl(FrontiersManager frontiersManager) {
        this.eventBus = new SimpleEventBus();
        this.frontiers = new ServerFrontierServiceImpl(frontiersManager, eventBus);
        this.permissions = new ServerPermissionServiceImpl(frontiersManager);
    }

    @Override
    public ServerFrontierService frontiers() {
        return frontiers;
    }

    @Override
    public ServerPermissionService permissions() {
        return permissions;
    }

    @Override
    public EventBus events() {
        return eventBus;
    }
}
