package games.alejandrocoria.mapfrontiers.common;

import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;

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
