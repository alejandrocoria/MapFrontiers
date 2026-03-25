package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.server.settings.ServerSettingsOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketRequestFrontierSettings {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_request_frontier_settings");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRequestFrontierSettings> STREAM_CODEC = StreamCodec.ofMember(PacketRequestFrontierSettings::encode, PacketRequestFrontierSettings::new);

    private int changeCounter;

    public PacketRequestFrontierSettings() {
        changeCounter = 0;
    }

    public PacketRequestFrontierSettings(int changeNonce) {
        this.changeCounter = changeNonce;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketRequestFrontierSettings(FriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > 1) {
                this.changeCounter = buf.readInt();
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketRequestFrontierSettings: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            buf.writeInt(changeCounter);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketRequestFrontierSettings: %s", t));
        }
    }

    public static void handle(PacketContext<PacketRequestFrontierSettings> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketRequestFrontierSettings message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerSettingsOperationResult result = MapFrontiers.getServerRuntime().getSettingsOperationService()
                    .requestSettings(player, message.changeCounter);
            result.dispatchNetworkActions();
        }
    }
}

