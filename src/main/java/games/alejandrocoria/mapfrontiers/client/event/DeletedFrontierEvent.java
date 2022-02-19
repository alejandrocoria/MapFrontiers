package games.alejandrocoria.mapfrontiers.client.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class DeletedFrontierEvent extends Event {
    public final UUID frontierID;

    public DeletedFrontierEvent(UUID frontierID) {
        this.frontierID = frontierID;
    }
}
