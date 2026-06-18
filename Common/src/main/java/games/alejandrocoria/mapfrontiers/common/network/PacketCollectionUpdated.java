package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
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

    public PacketCollectionUpdated(CollectionData collection) {
        this.collection = new CollectionData(collection);
    }

    @Override
    public CustomPacketPayload.Type<PacketCollectionUpdated> type() {
        return TYPE;
    }

    public PacketCollectionUpdated(FriendlyByteBuf buf) {
        this.collection = new CollectionData();
        if (buf.readableBytes() > 1) {
            this.collection.fromBytes(buf);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        collection.toBytes(buf);
    }

    public static void handle(PacketContext<PacketCollectionUpdated> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            MapFrontiersClient.applyCollectionUpdated(ctx.message().collection);
        }
    }
}
