package games.alejandrocoria.mapfrontiers.api.internal;

import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public interface PluginScopedServerFrontierService {
    FrontierDataView createGlobalFrontier(String pluginModId, UserRef owner, DimensionId dimension, FrontierShape shape);
    Optional<FrontierDataView> updateGlobalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation);
    boolean deleteGlobalFrontier(String pluginModId, FrontierId frontierId);
    List<FrontierDataView> listGlobalFrontiers(String pluginModId, DimensionId dimension);
    Optional<FrontierDataView> getFrontier(String pluginModId, FrontierId frontierId);
}
