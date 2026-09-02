package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.network.ClientPacketDelivery;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketFrontierSharingUpdated {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_frontier_sharing_updated");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFrontierSharingUpdated> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketFrontierSharingUpdated::encode, PacketFrontierSharingUpdated::new);

    private UUID frontierId = new UUID(0, 0);
    private ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath("minecraft", "overworld"));
    private FrontierSharingChange sharingChange = new FrontierSharingChange();
    private int playerId = -1;
    private long requestId;
    private OperationResolution resolution = OperationResolution.Accepted;

    public PacketFrontierSharingUpdated(UUID frontierId, ResourceKey<Level> dimension, FrontierSharingChange sharingChange) {
        this(frontierId, dimension, sharingChange, -1, 0L, OperationResolution.Accepted);
    }

    public PacketFrontierSharingUpdated(UUID frontierId, ResourceKey<Level> dimension, FrontierSharingChange sharingChange, int playerId) {
        this(frontierId, dimension, sharingChange, playerId, 0L, OperationResolution.Accepted);
    }

    public PacketFrontierSharingUpdated(UUID frontierId, ResourceKey<Level> dimension,
                                        FrontierSharingChange sharingChange, int playerId, long requestId,
                                        OperationResolution resolution) {
        this.frontierId = frontierId;
        this.dimension = dimension;
        this.sharingChange = new FrontierSharingChange(sharingChange);
        this.playerId = playerId;
        this.requestId = requestId;
        this.resolution = resolution;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketFrontierSharingUpdated(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 1) {
            this.frontierId = buf.readUUID();
            this.dimension = ResourceKey.create(Registries.DIMENSION, buf.readIdentifier());
            this.sharingChange = new FrontierSharingChange(buf);
            this.playerId = buf.readInt();
            requestId = buf.readLong();
            resolution = OperationResolution.VALUES[buf.readInt()];
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(frontierId);
        buf.writeIdentifier(dimension.identifier());
        sharingChange.toBytes(buf);
        buf.writeInt(playerId);
        buf.writeLong(requestId);
        buf.writeInt(resolution.ordinal());
    }

    public static void handle(PacketContext<PacketFrontierSharingUpdated> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            if (!MapFrontiersClient.isJourneyMapPluginAvailable()) {
                return;
            }
            PacketFrontierSharingUpdated message = ctx.message();
            ClientPacketDelivery.submit(() -> MapFrontiersClient.getOperationService()
                    .applyFrontierSharingUpdated(message.dimension, message.frontierId, message.sharingChange,
                            message.playerId, message.requestId, message.resolution));
        }
    }
}
