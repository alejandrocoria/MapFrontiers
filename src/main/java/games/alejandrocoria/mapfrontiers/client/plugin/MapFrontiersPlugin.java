package games.alejandrocoria.mapfrontiers.client.plugin;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.event.ClientEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@journeymap.client.api.ClientPlugin
@SideOnly(Side.CLIENT)
public class MapFrontiersPlugin implements IClientPlugin {
    @Override
    public void initialize(final IClientAPI jmAPI) {
        ((ClientProxy) MapFrontiers.proxy).setjmAPI(jmAPI);
    }

    @Override
    public String getModId() {
        return MapFrontiers.MODID;
    }

    @Override
    public void onEvent(ClientEvent event) {
    }
}