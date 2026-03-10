package games.alejandrocoria.mapfrontiers.api.internal;

import games.alejandrocoria.mapfrontiers.api.event.EventBus;

public interface InternalMapFrontiersClientAPI {
    PluginScopedClientFrontierService frontiers();
    EventBus events();
}
