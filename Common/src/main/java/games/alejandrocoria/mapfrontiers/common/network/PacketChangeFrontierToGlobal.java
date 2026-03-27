package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Date;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketChangeFrontierToGlobal {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_change_frontier_to_global");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketChangeFrontierToGlobal> STREAM_CODEC = StreamCodec.ofMember(PacketChangeFrontierToGlobal::encode, PacketChangeFrontierToGlobal::new);

    private UUID frontierID;
    private Date modified;

    public PacketChangeFrontierToGlobal(UUID frontierID, @Nullable Date modified) {
        this.frontierID = frontierID;
        this.modified = modified;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketChangeFrontierToGlobal(FriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > 1) {
                this.frontierID = UUIDHelper.fromBytes(buf);
                if (buf.readBoolean()) {
                    modified = new Date(buf.readLong());
                }
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketChangeFrontierToGlobal: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            UUIDHelper.toBytes(buf, frontierID);
            if (modified == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                buf.writeLong(modified.getTime());
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketChangeFrontierToGlobal: %s", t));
        }
    }

    public static void handle(PacketContext<PacketChangeFrontierToGlobal> ctx) {
        PacketChangeFrontierToGlobal message = ctx.message();

        if (Side.SERVER.equals(ctx.side())) {
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            if (MapFrontiers.getServerRuntime() == null) {
                return;
            }
            ServerFrontierOperationResult result = MapFrontiers.getServerRuntime().getOperationService().changeFrontierToGlobal(player, message.frontierID);
            result.dispatchNetworkActions();
        } else {
            if (!MapFrontiersClient.isJourneyMapPluginAvailable()) {
                return;
            }
            MapFrontiersClient.getOperationService().applyFrontierChangeToGlobal(message.frontierID, message.modified);
        }
    }
}
