package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.network.PlayerIdNetworkCodec;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketRemoveSharedUserPersonalFrontier {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_remove_shared_user_personal_frontier");

    private UUID frontierID;
    private final PlayerId targetUser;
    private long baseRevision;
    private long requestId;

    public PacketRemoveSharedUserPersonalFrontier(UUID frontierID, PlayerId targetUser,
                                                  long baseRevision, long requestId) {
        this.frontierID = frontierID;
        this.targetUser = targetUser;
        this.baseRevision = baseRevision;
        this.requestId = requestId;
    }

    public PacketRemoveSharedUserPersonalFrontier(FriendlyByteBuf buf) {
        this.frontierID = UUIDHelper.fromBytes(buf);
        this.targetUser = PlayerIdNetworkCodec.read(buf);
        baseRevision = buf.readLong();
        requestId = buf.readLong();
    }

    public void encode(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, frontierID);
        PlayerIdNetworkCodec.write(buf, targetUser);
        buf.writeLong(baseRevision);
        buf.writeLong(requestId);
    }

    public static void handle(PacketContext<PacketRemoveSharedUserPersonalFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketRemoveSharedUserPersonalFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            if (MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getShareService()
                    .removeSharedUserPersonalFrontier(player, message.frontierID, message.targetUser,
                            message.baseRevision, message.requestId);
            result.dispatchNetworkActions();
        }
    }
}
