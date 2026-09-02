package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.network.ClientPacketDelivery;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketCollectionUpdated implements CustomPacketPayload {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_collection_updated");
    public static final CustomPacketPayload.Type<PacketCollectionUpdated> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketCollectionUpdated> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketCollectionUpdated::encode, PacketCollectionUpdated::new);

    private final CollectionData collection;
    private int playerId = -1;
    private long requestId;
    private OperationResolution resolution = OperationResolution.Accepted;

    public PacketCollectionUpdated(CollectionData collection) {
        this(collection, -1, 0L, OperationResolution.Accepted);
    }

    public PacketCollectionUpdated(CollectionData collection, int playerId, long requestId,
                                   OperationResolution resolution) {
        this.collection = new CollectionData(collection);
        this.playerId = playerId;
        this.requestId = requestId;
        this.resolution = resolution;
    }

    @Override
    public CustomPacketPayload.Type<PacketCollectionUpdated> type() {
        return TYPE;
    }

    public PacketCollectionUpdated(FriendlyByteBuf buf) {
        this.collection = new CollectionData();
        if (buf.readableBytes() > 1) {
            this.collection.fromBytes(buf);
            playerId = buf.readInt();
            requestId = buf.readLong();
            resolution = OperationResolution.VALUES[buf.readInt()];
        }
    }

    public void encode(FriendlyByteBuf buf) {
        collection.toBytes(buf);
        buf.writeInt(playerId);
        buf.writeLong(requestId);
        buf.writeInt(resolution.ordinal());
    }

    public static void handle(PacketContext<PacketCollectionUpdated> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            PacketCollectionUpdated message = ctx.message();
            ClientPacketDelivery.submit(() -> MapFrontiersClient.applyCollectionUpdated(message.collection,
                    message.playerId, message.requestId, message.resolution));
        }
    }
}
