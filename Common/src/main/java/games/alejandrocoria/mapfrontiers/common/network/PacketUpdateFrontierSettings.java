package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.server.settings.ServerSettingsOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketUpdateFrontierSettings {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_update_frontier_settings");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateFrontierSettings> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketUpdateFrontierSettings::encode, PacketUpdateFrontierSettings::new);

    private final FrontierSettings settings;
    private long baseRevision;
    private long requestId;

    public PacketUpdateFrontierSettings(FrontierSettings settings, long baseRevision, long requestId) {
        this.settings = new FrontierSettings(settings);
        this.baseRevision = baseRevision;
        this.requestId = requestId;
    }

    public PacketUpdateFrontierSettings(FriendlyByteBuf buf) {
        settings = new FrontierSettings();
        if (buf.readableBytes() > 1) {
            settings.fromBytes(buf);
            baseRevision = buf.readLong();
            requestId = buf.readLong();
        }
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public void encode(FriendlyByteBuf buf) {
        settings.toBytes(buf);
        buf.writeLong(baseRevision);
        buf.writeLong(requestId);
    }

    public static void handle(PacketContext<PacketUpdateFrontierSettings> ctx) {
        if (!Side.SERVER.equals(ctx.side())) {
            return;
        }

        ServerPlayer player = ctx.sender();
        if (player == null || MapFrontiers.getServerRuntime() == null) {
            return;
        }

        PacketUpdateFrontierSettings message = ctx.message();
        ServerSettingsOperationResult result = MapFrontiers.getServerRuntime().getSettingsOperationService()
                .updateSettings(player, message.settings, message.baseRevision, message.requestId);
        result.dispatchNetworkActions();
    }
}
