package games.alejandrocoria.mapfrontiers.api.internal;

import games.alejandrocoria.mapfrontiers.api.event.EventBus;

public interface InternalMapFrontiersServerAPI {
    PluginScopedServerFrontierService frontiers();
    EventBus events();
}
