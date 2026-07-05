package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketCollectionUpdated {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_collection_updated");

    private final CollectionData collection;

    public PacketCollectionUpdated(CollectionData collection) {
        this.collection = new CollectionData(collection);
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
