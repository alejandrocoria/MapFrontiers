package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketCollectionDeleted {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_collection_deleted");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketCollectionDeleted> STREAM_CODEC = StreamCodec.ofMember(PacketCollectionDeleted::encode, PacketCollectionDeleted::new);

    private UUID collectionId = new UUID(0L, 0L);

    public PacketCollectionDeleted(UUID collectionId) {
        this.collectionId = collectionId;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketCollectionDeleted(FriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > 1) {
                this.collectionId = buf.readUUID();
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to read message for PacketCollectionDeleted", t);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            buf.writeUUID(collectionId);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to write message for PacketCollectionDeleted", t);
        }
    }

    public static void handle(PacketContext<PacketCollectionDeleted> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            MapFrontiersClient.applyCollectionDeleted(ctx.message().collectionId);
        }
    }
}
