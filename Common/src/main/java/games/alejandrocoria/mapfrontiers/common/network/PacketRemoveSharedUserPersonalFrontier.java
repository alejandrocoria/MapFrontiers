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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketRemoveSharedUserPersonalFrontier {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_remove_shared_user_personal_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRemoveSharedUserPersonalFrontier> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketRemoveSharedUserPersonalFrontier::encode, PacketRemoveSharedUserPersonalFrontier::new);

    private UUID frontierID;
    private final SettingsUser targetUser;

    public PacketRemoveSharedUserPersonalFrontier(UUID frontierID, SettingsUser user) {
        this.frontierID = frontierID;
        targetUser = user;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketRemoveSharedUserPersonalFrontier(FriendlyByteBuf buf) {
        this.targetUser = new SettingsUser();
        if (buf.readableBytes() > 1) {
            this.frontierID = UUIDHelper.fromBytes(buf);
            this.targetUser.fromBytes(buf);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, frontierID);
        targetUser.toBytes(buf);
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
                    .removeSharedUserPersonalFrontier(player, message.frontierID, message.targetUser);
            result.dispatchNetworkActions();
        }
    }
}
