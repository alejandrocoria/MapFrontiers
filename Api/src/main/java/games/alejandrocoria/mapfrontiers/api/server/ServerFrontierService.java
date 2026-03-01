package games.alejandrocoria.mapfrontiers.api.server;

import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;

import java.util.List;
import java.util.Optional;

public interface ServerFrontierService {
    // Global frontier
    FrontierDataView createGlobalFrontier(DimensionId dimension, FrontierShape shape);
    Optional<FrontierDataView> updateGlobalFrontier(FrontierId frontierId, FrontierMutation mutation);
    boolean deleteGlobalFrontier(FrontierId frontierId);
    Optional<FrontierDataView> changeGlobalToPersonal(FrontierId frontierId, UserRef newOwner); // global -> personal
    List<FrontierDataView> listGlobalFrontiers(DimensionId dimension);

    // Personal frontier conversion
    Optional<FrontierDataView> changePersonalToGlobal(FrontierId frontierId); // personal -> global

    // Any frontier
    Optional<FrontierDataView> getFrontier(FrontierId frontierId);
}
