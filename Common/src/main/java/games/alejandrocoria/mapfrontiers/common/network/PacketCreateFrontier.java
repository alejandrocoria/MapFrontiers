package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketCreateFrontier {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_create_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketCreateFrontier> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketCreateFrontier::encode, PacketCreateFrontier::new);

    private final FrontierCreateSpec createSpec;

    public PacketCreateFrontier(FrontierCreateSpec createSpec) {
        this.createSpec = createSpec;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketCreateFrontier(FriendlyByteBuf buf) {
        this.createSpec = FrontierCreateSpec.fromBytes(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        createSpec.toBytes(buf);
    }

    public static void handle(PacketContext<PacketCreateFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketCreateFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                MapFrontiers.LOGGER.warn("Ignoring PacketCreateFrontier because sender is null.");
                return;
            }
            if (MapFrontiers.getServerRuntime() == null) {
                return;
            }

            MapFrontiers.LOGGER.debug(
                "Handling PacketCreateFrontier from player={} frontierId={} personal={} sourcePluginId={}",
                    player.getGameProfile().name(), message.createSpec.getFrontierId(), message.createSpec.isPersonal(),
                    message.createSpec.getSourcePluginId()
            );

            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService().createFrontier(player, message.createSpec);
            if (!result.isSuccess()) {
                MapFrontiers.LOGGER.warn(
                        "Rejected PacketCreateFrontier from player={} frontierId={} personal={} sourcePluginId={}",
                        player.getGameProfile().name(), message.createSpec.getFrontierId(), message.createSpec.isPersonal(),
                        message.createSpec.getSourcePluginId()
                );
            }
            result.dispatchNetworkActions();
        }
    }
}
