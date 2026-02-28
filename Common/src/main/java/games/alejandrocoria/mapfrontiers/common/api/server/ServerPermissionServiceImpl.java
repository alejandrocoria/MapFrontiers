package games.alejandrocoria.mapfrontiers.common.api.server;

import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.api.server.ServerPermissionService;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;

public class ServerPermissionServiceImpl implements ServerPermissionService {
    private final FrontiersManager frontiersManager;

    public ServerPermissionServiceImpl(FrontiersManager frontiersManager) {
        this.frontiersManager = frontiersManager;
    }

    @Override
    public boolean canCreateGlobalFrontier(UserRef userRef) {
        SettingsUser user = new SettingsUser();
        user.uuid = userRef.id();
        user.username = userRef.name() == null ? "" : userRef.name();
        return frontiersManager.getSettings().checkAction(FrontierSettings.Action.CreateGlobalFrontier, user, false, null);
    }
}
