package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.frontier.server.ServerFrontierCommandResult;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
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
public class PacketChangeFrontierToPersonal {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_change_frontier_to_personal");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketChangeFrontierToPersonal> STREAM_CODEC = StreamCodec.ofMember(PacketChangeFrontierToPersonal::encode, PacketChangeFrontierToPersonal::new);

    private UUID frontierID;
    private Date modified;

    public PacketChangeFrontierToPersonal(UUID frontierID, @Nullable Date modified) {
        this.frontierID = frontierID;
        this.modified = modified;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketChangeFrontierToPersonal(FriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > 1) {
                this.frontierID = UUIDHelper.fromBytes(buf);
                if (buf.readBoolean()) {
                    modified = new Date(buf.readLong());
                }
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketChangeFrontierToPersonal: %s", t));
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
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketChangeFrontierToPersonal: %s", t));
        }
    }

    public static void handle(PacketContext<PacketChangeFrontierToPersonal> ctx) {
        PacketChangeFrontierToPersonal message = ctx.message();

        if (Side.SERVER.equals(ctx.side())) {
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            if (MapFrontiers.getServerRuntime() == null) {
                return;
            }
            ServerFrontierCommandResult result = MapFrontiers.getServerRuntime().getCommandService().changeFrontierToPersonal(player, message.frontierID);
            result.dispatchNetworkActions();
        } else {
            MapFrontiersClient.getCommandService().applyFrontierChangeToPersonal(message.frontierID, message.modified);
        }
    }
}
