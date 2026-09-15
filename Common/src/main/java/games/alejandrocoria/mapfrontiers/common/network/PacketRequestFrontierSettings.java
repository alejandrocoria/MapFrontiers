package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.server.settings.ServerSettingsOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketRequestFrontierSettings {
    public static final long UNKNOWN_REVISION = -1L;
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_request_frontier_settings");

    private long settingsRevision;

    public PacketRequestFrontierSettings() {
        settingsRevision = UNKNOWN_REVISION;
    }

    public PacketRequestFrontierSettings(long settingsRevision) {
        this.settingsRevision = settingsRevision;
    }

    public PacketRequestFrontierSettings(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 1) {
            this.settingsRevision = buf.readLong();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(settingsRevision);
    }

    public static void handle(PacketContext<PacketRequestFrontierSettings> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketRequestFrontierSettings message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerSettingsOperationResult result = MapFrontiers.getServerRuntime().getSettingsOperationService()
                    .requestSettings(player, message.settingsRevision);
            result.dispatchNetworkActions();
        }
    }
}
