package games.alejandrocoria.mapfrontiers.common.event;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class UpdatedSettingsProfileEvent extends Event {
    public final SettingsProfile profile;

    public UpdatedSettingsProfileEvent(SettingsProfile profile) {
        this.profile = profile;
    }
}
