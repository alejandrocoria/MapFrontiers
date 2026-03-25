package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketDeleteFrontier {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_delete_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketDeleteFrontier> STREAM_CODEC = StreamCodec.ofMember(PacketDeleteFrontier::encode, PacketDeleteFrontier::new);

    private UUID frontierID;

    public PacketDeleteFrontier(UUID frontierID) {
        this.frontierID = frontierID;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketDeleteFrontier(FriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > 1) {
                this.frontierID = UUIDHelper.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketDeleteFrontier: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            UUIDHelper.toBytes(buf, frontierID);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketDeleteFrontier: %s", t));
        }
    }

    public static void handle(PacketContext<PacketDeleteFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketDeleteFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            if (MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerFrontierOperationResult result = MapFrontiers.getServerRuntime().getOperationService().deleteFrontier(player, message.frontierID);
            result.dispatchNetworkActions();
        }
    }
}
