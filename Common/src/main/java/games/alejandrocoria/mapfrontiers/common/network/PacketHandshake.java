package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketHandshake {
    private static final String VERSION = "1";

    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_handshake");

    private String version;

    public PacketHandshake() {
        this.version = VERSION;
    }

    public static PacketHandshake decode(FriendlyByteBuf buf) {
        PacketHandshake packet = new PacketHandshake();

        try {
            if (buf.readableBytes() > 1) {
                packet.version = buf.readUtf();
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketHandshake: %s", t));
        }

        return packet;
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            buf.writeUtf(version);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketHandshake: %s", t));
        }
    }

    public static void handle(PacketContext<PacketHandshake> ctx) {
        // No version check at the moment.

        if (Side.SERVER.equals(ctx.side())) {
            ServerPlayer player = (ServerPlayer) ctx.sender();
            if (player == null) {
                return;
            }
            MapFrontiers.ReceiveHandshake(player);
        }
    }
}
