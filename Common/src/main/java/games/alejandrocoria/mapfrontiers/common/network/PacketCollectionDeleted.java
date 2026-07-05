package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketCollectionDeleted {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_collection_deleted");

    private UUID collectionId = new UUID(0L, 0L);

    public PacketCollectionDeleted(UUID collectionId) {
        this.collectionId = collectionId;
    }

    public PacketCollectionDeleted(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 1) {
            this.collectionId = buf.readUUID();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(collectionId);
    }

    public static void handle(PacketContext<PacketCollectionDeleted> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            MapFrontiersClient.applyCollectionDeleted(ctx.message().collectionId);
        }
    }
}
