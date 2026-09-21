package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.network.ClientPacketDelivery;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketCollectionCreated {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_collection_created");

    private final CollectionData collection;

    public PacketCollectionCreated(CollectionData collection) {
        this.collection = new CollectionData(collection);
    }

    public PacketCollectionCreated(FriendlyByteBuf buf) {
        this.collection = CollectionData.fromBytes(buf);
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
