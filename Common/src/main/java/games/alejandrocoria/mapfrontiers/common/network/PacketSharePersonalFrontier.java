package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketSharePersonalFrontier implements CustomPacketPayload {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_share_personal_frontier");
    public static final CustomPacketPayload.Type<PacketSharePersonalFrontier> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSharePersonalFrontier> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketSharePersonalFrontier::encode, PacketSharePersonalFrontier::new);

    private UUID frontierID;
    private final SettingsUserShared userShared;
    private long baseRevision;
    private long requestId;

    public PacketSharePersonalFrontier() {
        userShared = new SettingsUserShared();
    }

    public PacketSharePersonalFrontier(UUID frontierID, SettingsUser user, long baseRevision, long requestId) {
        this(frontierID, createSharedUser(user), baseRevision, requestId);
    }

    public PacketSharePersonalFrontier(UUID frontierID, SettingsUserShared userShared, long baseRevision, long requestId) {
        this.frontierID = frontierID;
        this.userShared = new SettingsUserShared(userShared);
        this.baseRevision = baseRevision;
        this.requestId = requestId;
    }

    @Override
    public CustomPacketPayload.Type<PacketSharePersonalFrontier> type() {
        return TYPE;
    }

    public PacketSharePersonalFrontier(FriendlyByteBuf buf) {
        this.userShared = new SettingsUserShared();
        if (buf.readableBytes() > 1) {
            this.frontierID = UUIDHelper.fromBytes(buf);
            this.userShared.fromBytes(buf);
            baseRevision = buf.readLong();
            requestId = buf.readLong();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, frontierID);
        userShared.toBytes(buf);
        buf.writeLong(baseRevision);
        buf.writeLong(requestId);
    }

    public static void handle(PacketContext<PacketSharePersonalFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketSharePersonalFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            if (MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getShareService()
                    .sharePersonalFrontier(player, message.frontierID, message.userShared,
                            message.baseRevision, message.requestId);
            result.dispatchNetworkActions();
        }
    }

    private static SettingsUserShared createSharedUser(SettingsUser user) {
        SettingsUserShared sharedUser = new SettingsUserShared(user, false);
        sharedUser.setActions(EnumSet.noneOf(SettingsUserShared.Action.class));
        return sharedUser;
    }
}
