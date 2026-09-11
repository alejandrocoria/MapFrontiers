package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerReferenceCollector;
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
public class PacketPersonalCollection implements CustomPacketPayload {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_personal_collection");
    public static final CustomPacketPayload.Type<PacketPersonalCollection> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketPersonalCollection> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketPersonalCollection::encode, PacketPersonalCollection::new);

    private final CollectionData collection;
    private final PacketPlayerNameMappings playerNameMappings;

    public PacketPersonalCollection(CollectionData collection, PlayerNameResolver playerNameResolver) {
        this.collection = new CollectionData(collection);
        playerNameMappings = new PacketPlayerNameMappings(PlayerReferenceCollector.collect(collection), playerNameResolver);
    }

    @Override
    public CustomPacketPayload.Type<PacketPersonalCollection> type() {
        return TYPE;
    }

    public PacketPersonalCollection(FriendlyByteBuf buf) {
        this.collection = CollectionData.fromBytes(buf);
        playerNameMappings = new PacketPlayerNameMappings(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        collection.toBytes(buf);
        playerNameMappings.encode(buf);
    }

    public static void handle(PacketContext<PacketPersonalCollection> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketPersonalCollection message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }
            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService().importPersonalCollection(player, message.collection);
            if (result.isSuccess() && result.getCollection() != null) {
                message.playerNameMappings.applyHintsTo(MapFrontiers.getServerRuntime().getPlayerNameRepository(),
                        PlayerReferenceCollector.collect(result.getCollection()));
            }
            result.dispatchNetworkActions();
        }
    }

    CollectionData getCollection() {
        return collection;
    }

    PacketPlayerNameMappings getPlayerNameMappings() {
        return playerNameMappings;
    }
}
