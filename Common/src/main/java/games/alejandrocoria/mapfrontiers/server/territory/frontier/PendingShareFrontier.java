package games.alejandrocoria.mapfrontiers.server.territory.frontier;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PendingShareFrontier {
    public final UUID frontierID;
    public final PlayerId targetUser;
    public int tickCount;

    public PendingShareFrontier(UUID frontierID, PlayerId targetUser) {
        this.frontierID = frontierID;
        this.targetUser = targetUser;
        tickCount = 0;
    }
}
