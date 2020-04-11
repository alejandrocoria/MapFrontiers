package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(MapFrontiers.MODID);

    public static void init() {
        int id = 0;
        INSTANCE.registerMessage(PacketFrontier.Handler.class, PacketFrontier.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketNewFrontier.Handler.class, PacketNewFrontier.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketDeleteFrontier.Handler.class, PacketDeleteFrontier.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketFrontierDeleted.Handler.class, PacketFrontierDeleted.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketUpdateFrontier.Handler.class, PacketUpdateFrontier.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketFrontierUpdated.Handler.class, PacketFrontierUpdated.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketRequestFrontierSettings.Handler.class, PacketRequestFrontierSettings.class, id++,
                Side.SERVER);
        INSTANCE.registerMessage(PacketFrontierSettings.Handler.class, PacketFrontierSettings.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketFrontierSettings.Handler.class, PacketFrontierSettings.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketSettingsProfile.Handler.class, PacketSettingsProfile.class, id++, Side.CLIENT);
    }
}
