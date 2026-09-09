package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerReferenceCollector;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketPersonalFrontier implements CustomPacketPayload {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_personal_frontier");
    public static final CustomPacketPayload.Type<PacketPersonalFrontier> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketPersonalFrontier> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketPersonalFrontier::encode, PacketPersonalFrontier::new);

    private final FrontierData frontier;
    private final PacketPlayerNameMappings playerNameMappings;

    public PacketPersonalFrontier(FrontierData frontier, PlayerNameResolver playerNameResolver) {
        this.frontier = frontier;
        playerNameMappings = new PacketPlayerNameMappings(PlayerReferenceCollector.collect(frontier), playerNameResolver);
    }

    @Override
    public CustomPacketPayload.Type<PacketPersonalFrontier> type() {
        return TYPE;
    }

    public PacketPersonalFrontier(FriendlyByteBuf buf) {
        this.frontier = FrontierData.fromBytes(buf);
        playerNameMappings = new PacketPlayerNameMappings(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        frontier.toBytes(buf);
        playerNameMappings.encode(buf);
    }

    public static void handle(PacketContext<PacketPersonalFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketPersonalFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }
            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService().importPersonalFrontier(player, message.frontier);
            if (result.isSuccess() && result.getFrontier() != null) {
                message.playerNameMappings.applyHintsTo(MapFrontiers.getServerRuntime().getPlayerNameRepository(),
                        PlayerReferenceCollector.collect(result.getFrontier()));
            }
            result.dispatchNetworkActions();
        }
    }

    FrontierData getFrontier() {
        return frontier;
    }

    PacketPlayerNameMappings getPlayerNameMappings() {
        return playerNameMappings;
    }
}
