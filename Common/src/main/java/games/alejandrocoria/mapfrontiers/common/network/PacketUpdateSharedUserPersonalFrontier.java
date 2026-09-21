package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
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
    private final FrontierUserAccess userShared;
    private long baseRevision;
    private long requestId;

    public PacketUpdateSharedUserPersonalFrontier(UUID frontierID, FrontierUserAccess user,
                                                  long baseRevision, long requestId) {
        this.frontierID = frontierID;
        userShared = new FrontierUserAccess(user);
        this.baseRevision = baseRevision;
        this.requestId = requestId;
    }

    public PacketUpdateSharedUserPersonalFrontier(FriendlyByteBuf buf) {
        this.frontierID = UUIDHelper.fromBytes(buf);
        this.userShared = FrontierUserAccess.fromBytes(buf);
        baseRevision = buf.readLong();
        requestId = buf.readLong();
    }

    public void encode(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, frontierID);
        userShared.toBytes(buf);
        buf.writeLong(baseRevision);
        buf.writeLong(requestId);
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
                    .updateSharedUserPersonalFrontier(player, message.frontierID, message.userShared,
                            message.baseRevision, message.requestId);
            result.dispatchNetworkActions();
        }
    }
}
