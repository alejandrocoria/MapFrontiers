package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketHandshake {
    private static final String VERSION = "1";

    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_handshake");

    private long nonce;
    private String version;

    public PacketHandshake(long nonce) {
        this.nonce = nonce;
        this.version = VERSION;
    }

    public PacketHandshake(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 0) {
            this.nonce = buf.readLong();
            this.version = buf.readUtf();
        } else {
            this.nonce = 0L;
            this.version = VERSION;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(nonce);
        buf.writeUtf(version);
    }

    public static void handle(PacketContext<PacketHandshake> ctx) {
        PacketHandshake message = ctx.message();
        if (Side.SERVER.equals(ctx.side())) {
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            MapFrontiers.ReceiveHandshake(player, message.nonce);
        } else if (Side.CLIENT.equals(ctx.side())) {
            MapFrontiersClient.receiveHandshakeAck(message.nonce);
        }
    }
}
