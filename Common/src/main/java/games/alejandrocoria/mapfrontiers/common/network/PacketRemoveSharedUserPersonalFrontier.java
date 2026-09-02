package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketRemoveSharedUserPersonalFrontier {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_remove_shared_user_personal_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRemoveSharedUserPersonalFrontier> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketRemoveSharedUserPersonalFrontier::encode, PacketRemoveSharedUserPersonalFrontier::new);

    private UUID frontierID;
    private final SettingsUser targetUser;
    private long baseRevision;
    private long requestId;

    public PacketRemoveSharedUserPersonalFrontier(UUID frontierID, SettingsUser user,
                                                  long baseRevision, long requestId) {
        this.frontierID = frontierID;
        targetUser = new SettingsUser(user);
        this.baseRevision = baseRevision;
        this.requestId = requestId;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketRemoveSharedUserPersonalFrontier(FriendlyByteBuf buf) {
        this.targetUser = new SettingsUser();
        if (buf.readableBytes() > 1) {
            this.frontierID = UUIDHelper.fromBytes(buf);
            this.targetUser.fromBytes(buf);
            baseRevision = buf.readLong();
            requestId = buf.readLong();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, frontierID);
        targetUser.toBytes(buf);
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
