package games.alejandrocoria.mapfrontiers.api.client;

import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.SharedUserAccess;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;

import java.util.List;
import java.util.Optional;

public interface ClientFrontierService {
    // Any frontier
    Optional<FrontierDataView> getFrontier(FrontierId frontierId);

    // Global frontier
    FrontierActionResult createGlobalFrontier(DimensionId dimension, FrontierShape shape);
    FrontierActionResult updateGlobalFrontier(FrontierId frontierId, FrontierMutation mutation);
    FrontierActionResult deleteGlobalFrontier(FrontierId frontierId);
    FrontierActionResult changeToPersonal(FrontierId frontierId); // global -> personal
    List<FrontierDataView> listGlobalFrontiers(DimensionId dimension);

    // Personal frontier
    FrontierActionResult createPersonalFrontier(DimensionId dimension, FrontierShape shape);
    FrontierActionResult updatePersonalFrontier(FrontierId frontierId, FrontierMutation mutation);
    FrontierActionResult deletePersonalFrontier(FrontierId frontierId);
    FrontierActionResult changeToGlobal(FrontierId frontierId);   // personal -> global
    List<FrontierDataView> listPersonalFrontiers(DimensionId dimension);

    // Personal sharing
    FrontierActionResult sharePersonalFrontier(FrontierId frontierId, SharedUserAccess sharedUserAccess);
    FrontierActionResult updateSharedUserAccess(FrontierId frontierId, SharedUserAccess sharedUserAccess);
    FrontierActionResult removeSharedUser(FrontierId frontierId, UserRef user);
}
