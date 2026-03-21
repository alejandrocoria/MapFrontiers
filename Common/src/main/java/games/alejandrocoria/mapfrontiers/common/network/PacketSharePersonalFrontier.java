package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.frontier.server.ServerFrontierOperationResult;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
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
public class PacketSharePersonalFrontier {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_share_personal_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSharePersonalFrontier> STREAM_CODEC = StreamCodec.ofMember(PacketSharePersonalFrontier::encode, PacketSharePersonalFrontier::new);

    private UUID frontierID;
    private final SettingsUserShared userShared;

    public PacketSharePersonalFrontier() {
        userShared = new SettingsUserShared();
    }

    public PacketSharePersonalFrontier(UUID frontierID, SettingsUser user) {
        this(frontierID, createSharedUser(user));
    }

    public PacketSharePersonalFrontier(UUID frontierID, SettingsUserShared userShared) {
        this.frontierID = frontierID;
        this.userShared = userShared;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketSharePersonalFrontier(FriendlyByteBuf buf) {
        this.userShared = new SettingsUserShared();

        try {
            if (buf.readableBytes() > 1) {
                this.frontierID = UUIDHelper.fromBytes(buf);
                this.userShared.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketSharePersonalFrontier: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            UUIDHelper.toBytes(buf, frontierID);
            userShared.toBytes(buf);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketSharePersonalFrontier: %s", t));
        }
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

            ServerFrontierOperationResult result = MapFrontiers.getServerRuntime().getShareService()
                    .sharePersonalFrontier(player, message.frontierID, message.userShared);
            result.dispatchNetworkActions();
        }
    }

    private static SettingsUserShared createSharedUser(SettingsUser user) {
        SettingsUserShared sharedUser = new SettingsUserShared(user, false);
        sharedUser.setActions(EnumSet.noneOf(SettingsUserShared.Action.class));
        return sharedUser;
    }
}
