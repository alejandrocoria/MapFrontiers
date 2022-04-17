package games.alejandrocoria.mapfrontiers.common;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PendingShareFrontier {
    public final UUID frontierID;
    public final SettingsUser targetUser;
    public int tickCount;

    public PendingShareFrontier(UUID frontierID, SettingsUser targetUser) {
        this.frontierID = frontierID;
        this.targetUser = targetUser;
        tickCount = 0;
    }
}
