package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChange;
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
public class PacketUpdateFrontier implements CustomPacketPayload {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_update_frontier");
    public static final CustomPacketPayload.Type<PacketUpdateFrontier> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateFrontier> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketUpdateFrontier::encode, PacketUpdateFrontier::new);

    private UUID frontierId = new UUID(0, 0);
    private FrontierChange change = new FrontierChange();
    private long expectedSyncHash;

    public PacketUpdateFrontier(UUID frontierId, FrontierChange change, long expectedSyncHash) {
        this.frontierId = frontierId;
        this.change = change;
        this.expectedSyncHash = expectedSyncHash;
    }

    @Override
    public CustomPacketPayload.Type<PacketUpdateFrontier> type() {
        return TYPE;
    }

    public PacketUpdateFrontier(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 1) {
            this.frontierId = buf.readUUID();
            this.change = new FrontierChange(buf);
            this.expectedSyncHash = buf.readLong();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(frontierId);
        change.toBytes(buf);
        buf.writeLong(expectedSyncHash);
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

            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService()
                    .updateFrontier(player, message.frontierId, message.change, message.expectedSyncHash);
            result.dispatchNetworkActions();
        }
    }
}
