package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketCreateCollection {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_create_collection");

    private final CollectionData collection;

    public PacketCreateCollection(CollectionData collection) {
        this.collection = new CollectionData(collection);
    }

    public PacketCreateCollection(FriendlyByteBuf buf) {
        this.collection = new CollectionData();
        if (buf.readableBytes() > 1) {
            this.collection.fromBytes(buf);
        }
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
