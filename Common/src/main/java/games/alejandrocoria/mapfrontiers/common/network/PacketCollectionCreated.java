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
public class PacketCollectionCreated {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_collection_created");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketCollectionCreated> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketCollectionCreated::encode, PacketCollectionCreated::new);

    private final CollectionData collection;

    public PacketCollectionCreated(CollectionData collection) {
        this.collection = new CollectionData(collection);
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketCollectionCreated(FriendlyByteBuf buf) {
        this.collection = new CollectionData();
        if (buf.readableBytes() > 1) {
            this.collection.fromBytes(buf);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        collection.toBytes(buf);
    }

    public static void handle(PacketContext<PacketCollectionCreated> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            PacketCollectionCreated message = ctx.message();
            ClientPacketDelivery.submit(() -> MapFrontiersClient.applyCollectionCreated(message.collection));
        }
    }
}
