package games.alejandrocoria.mapfrontiers.common.network;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
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
        INSTANCE.registerMessage(PacketSharePersonalFrontier.Handler.class, PacketSharePersonalFrontier.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketPersonalFrontierShared.Handler.class, PacketPersonalFrontierShared.class, id++,
                Side.CLIENT);
        INSTANCE.registerMessage(PacketRemoveSharedUserPersonalFrontier.Handler.class,
                PacketRemoveSharedUserPersonalFrontier.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketUpdateSharedUserPersonalFrontier.Handler.class,
                PacketUpdateSharedUserPersonalFrontier.class, id++, Side.SERVER);
    }

    public static void sendToUsersWithAccess(IMessage message, FrontierData frontier) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        EntityPlayerMP player = (EntityPlayerMP) server.getEntityFromUuid(frontier.getOwner().uuid);
        if (player != null) {
            INSTANCE.sendTo(message, player);
        }

        List<SettingsUserShared> usersShared = frontier.getUsersShared();
        if (usersShared != null) {
            for (SettingsUserShared userShared : usersShared) {
                if (!userShared.isPending()) {
                    player = (EntityPlayerMP) server.getEntityFromUuid(userShared.getUser().uuid);
                    if (player != null) {
                        INSTANCE.sendTo(message, player);
                    }
                }
            }
        }
    }
}
