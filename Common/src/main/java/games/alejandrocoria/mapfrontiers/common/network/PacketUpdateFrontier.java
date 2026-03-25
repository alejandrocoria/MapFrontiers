package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
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
public class PacketUpdateFrontier {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_update_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateFrontier> STREAM_CODEC = StreamCodec.ofMember(PacketUpdateFrontier::encode, PacketUpdateFrontier::new);

    private UUID frontierId = new UUID(0, 0);
    private FrontierChange change = new FrontierChange();

    public PacketUpdateFrontier(UUID frontierId, FrontierChange change) {
        this.frontierId = frontierId;
        this.change = change;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketUpdateFrontier(FriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > 1) {
                this.frontierId = buf.readUUID();
                this.change = new FrontierChange(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketUpdateFrontier: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            buf.writeUUID(frontierId);
            change.toBytes(buf);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketUpdateFrontier: %s", t));
        }
    }

    public static void handle(PacketContext<PacketUpdateFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketUpdateFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            if (MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerFrontierOperationResult result = MapFrontiers.getServerRuntime().getOperationService()
                    .updateFrontier(player, message.frontierId, message.change);
            result.dispatchNetworkActions();
        }
    }
}
