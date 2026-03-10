package games.alejandrocoria.mapfrontiers.common.api.server;

import games.alejandrocoria.mapfrontiers.api.event.EventBus;
import games.alejandrocoria.mapfrontiers.api.internal.InternalMapFrontiersServerAPI;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerFrontierService;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;

public class MapFrontiersServerAPIImpl implements InternalMapFrontiersServerAPI {
    private final PluginScopedServerFrontierService frontiers;
    private final SimpleEventBus eventBus;

    public MapFrontiersServerAPIImpl(FrontiersManager frontiersManager) {
        this.eventBus = new SimpleEventBus();
        this.frontiers = new ServerFrontierServiceImpl(frontiersManager, eventBus);
    }

    @Override
    public PluginScopedServerFrontierService frontiers() {
        return frontiers;
    }

    @Override
    public EventBus events() {
        return eventBus;
    }
}
