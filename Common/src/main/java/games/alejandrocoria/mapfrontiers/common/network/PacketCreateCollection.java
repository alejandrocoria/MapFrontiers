package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketCreateCollection implements CustomPacketPayload {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_create_collection");
    public static final CustomPacketPayload.Type<PacketCreateCollection> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketCreateCollection> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketCreateCollection::encode, PacketCreateCollection::new);

    private final CollectionData collection;

    public PacketCreateCollection(CollectionData collection) {
        this.collection = new CollectionData(collection);
    }

    @Override
    public CustomPacketPayload.Type<PacketCreateCollection> type() {
        return TYPE;
    }

    public PacketCreateCollection(FriendlyByteBuf buf) {
        this.collection = CollectionData.fromBytes(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        collection.toBytes(buf);
    }

    public static void handle(PacketContext<PacketCreateCollection> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketCreateCollection message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService().createCollection(player, message.collection);
            result.dispatchNetworkActions();
        }
    }
}
