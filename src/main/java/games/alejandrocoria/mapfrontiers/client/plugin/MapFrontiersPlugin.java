package games.alejandrocoria.mapfrontiers.client.plugin;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.event.ClientEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@journeymap.client.api.ClientPlugin
@OnlyIn(Dist.CLIENT)
public class MapFrontiersPlugin implements IClientPlugin {
    @Override
    public void initialize(final IClientAPI jmAPI) {
        ClientProxy.setjmAPI(jmAPI);
    }

    @Override
    public String getModId() {
        return MapFrontiers.MODID;
    }

    @Override
    public void onEvent(ClientEvent event) {
    }
}