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
    Optional<FrontierDataView> getFrontier(FrontierId frontierId);
    Optional<FrontierDataView> createPersonalFrontier(DimensionId dimension, FrontierShape shape);
    Optional<FrontierDataView> updateFrontier(FrontierId frontierId, FrontierMutation mutation);
    boolean deleteFrontier(FrontierId frontierId);
    Optional<FrontierDataView> updatePersonalFrontier(FrontierId frontierId, FrontierMutation mutation);
    boolean deletePersonalFrontier(FrontierId frontierId);
    List<FrontierDataView> listPersonalFrontiers(DimensionId dimension);
    Optional<FrontierDataView> changeToGlobal(FrontierId frontierId);
    Optional<FrontierDataView> changeToPersonal(FrontierId frontierId);
    Optional<FrontierDataView> sharePersonalFrontier(FrontierId frontierId, SharedUserAccess sharedUserAccess);
    Optional<FrontierDataView> updateSharedUserAccess(FrontierId frontierId, SharedUserAccess sharedUserAccess);
    Optional<FrontierDataView> removeSharedUser(FrontierId frontierId, UserRef user);
}
