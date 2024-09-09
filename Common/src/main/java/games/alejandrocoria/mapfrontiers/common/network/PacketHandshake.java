package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketHandshake {
    private static final String VERSION = "1";

    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_handshake");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketHandshake> STREAM_CODEC = StreamCodec.ofMember(PacketHandshake::encode, PacketHandshake::new);

    private String version;

    public PacketHandshake() {
        this.version = VERSION;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketHandshake(FriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > 1) {
                this.version = buf.readUtf();
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketHandshake: %s", t));
        }
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
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            MapFrontiers.ReceiveHandshake(player);
        }
    }
}
