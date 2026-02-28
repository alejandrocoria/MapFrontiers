package games.alejandrocoria.mapfrontiers.api.server;

import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.SharedUserAccess;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;

import java.util.List;
import java.util.Optional;

public interface ServerFrontierService {
    FrontierDataView createGlobalFrontier(DimensionId dimension, FrontierShape shape, UserRef user);
    Optional<FrontierDataView> updateGlobalFrontier(FrontierId frontierId, FrontierMutation mutation, UserRef user);
    boolean deleteGlobalFrontier(FrontierId frontierId, UserRef user);
    Optional<FrontierDataView> changeGlobalToPersonal(FrontierId frontierId, UserRef newOwner, UserRef user);
    Optional<FrontierDataView> changePersonalToGlobal(FrontierId frontierId, UserRef user);
    Optional<FrontierDataView> sharePersonalFrontier(FrontierId frontierId, SharedUserAccess sharedUserAccess, UserRef user);
    Optional<FrontierDataView> updateSharedUserAccess(FrontierId frontierId, SharedUserAccess sharedUserAccess, UserRef user);
    Optional<FrontierDataView> removeSharedUser(FrontierId frontierId, UserRef targetUser, UserRef user);
    Optional<FrontierDataView> getFrontier(FrontierId frontierId);
    List<FrontierDataView> listGlobalFrontiers(DimensionId dimension);
}
