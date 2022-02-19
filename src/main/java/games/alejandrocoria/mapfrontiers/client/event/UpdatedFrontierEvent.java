package games.alejandrocoria.mapfrontiers.client.event;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class UpdatedFrontierEvent extends Event {
    public final FrontierOverlay frontierOverlay;
    public final int playerID;

    public UpdatedFrontierEvent(FrontierOverlay frontierOverlay, int playerID) {
        this.frontierOverlay = frontierOverlay;
        this.playerID = playerID;
    }
}
