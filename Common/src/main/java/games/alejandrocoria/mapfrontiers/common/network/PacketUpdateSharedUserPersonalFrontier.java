package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketUpdateSharedUserPersonalFrontier {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_update_shared_user_personal_frontier");

    private UUID frontierID;
    private final SettingsUserShared userShared;

    public PacketUpdateSharedUserPersonalFrontier(UUID frontierID, SettingsUserShared user) {
        this.frontierID = frontierID;
        userShared = user;
    }

    public PacketUpdateSharedUserPersonalFrontier(FriendlyByteBuf buf) {
        this.userShared = new SettingsUserShared();
        if (buf.readableBytes() > 1) {
            this.frontierID = UUIDHelper.fromBytes(buf);
            this.userShared.fromBytes(buf);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, frontierID);
        userShared.toBytes(buf);
    }

    public static void handle(PacketContext<PacketUpdateSharedUserPersonalFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketUpdateSharedUserPersonalFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            if (MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getShareService()
                    .updateSharedUserPersonalFrontier(player, message.frontierID, message.userShared);
            result.dispatchNetworkActions();
        }
    }
}
