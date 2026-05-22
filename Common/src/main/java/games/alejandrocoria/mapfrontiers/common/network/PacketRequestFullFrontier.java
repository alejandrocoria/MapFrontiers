package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketRequestFullFrontier {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_request_full_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRequestFullFrontier> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketRequestFullFrontier::encode, PacketRequestFullFrontier::new);

    private UUID frontierId = new UUID(0, 0);

    public PacketRequestFullFrontier(UUID frontierId) {
        this.frontierId = frontierId;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketRequestFullFrontier(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 1) {
            frontierId = buf.readUUID();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(frontierId);
    }

    public static void handle(PacketContext<PacketRequestFullFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketRequestFullFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService()
                    .requestFullFrontier(player, message.frontierId);
            result.dispatchNetworkActions();
        }
    }
}
