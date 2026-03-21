package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.frontier.server.ServerFrontierOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
public class PacketUpdateFrontier {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_update_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateFrontier> STREAM_CODEC = StreamCodec.ofMember(PacketUpdateFrontier::encode, PacketUpdateFrontier::new);

    private final FrontierData frontier;
    private final Set<FrontierData.Change> changes;

    public PacketUpdateFrontier(FrontierData frontier) {
        this.frontier = frontier;
        changes = frontier.getChanges();
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketUpdateFrontier(FriendlyByteBuf buf) {
        this.frontier = new FrontierData();
        this.changes = null;

        try {
            if (buf.readableBytes() > 1) {
                this.frontier.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketUpdateFrontier: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            frontier.toBytes(buf, changes);
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

            ServerFrontierOperationResult result = MapFrontiers.getServerRuntime().getOperationService().updateFrontier(player, message.frontier);
            result.dispatchNetworkActions();
        }
    }
}
