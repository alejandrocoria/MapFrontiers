package games.alejandrocoria.mapfrontiers.api.server;

import games.alejandrocoria.mapfrontiers.api.model.UserRef;

public interface ServerPermissionService {
    boolean canCreateGlobalFrontier(UserRef user);
}
