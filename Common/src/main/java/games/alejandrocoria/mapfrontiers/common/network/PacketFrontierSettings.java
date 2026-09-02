package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.screen.page.ModSettingsPage;
import games.alejandrocoria.mapfrontiers.client.network.ClientPacketDelivery;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFrontierSettings {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_frontier_settings");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFrontierSettings> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketFrontierSettings::encode, PacketFrontierSettings::new);

    private final FrontierSettings settings;
    private long settingsRevision;
    private long requestId;
    private OperationResolution resolution = OperationResolution.Accepted;

    public PacketFrontierSettings(FrontierSettings settings, long settingsRevision, long requestId,
                                  OperationResolution resolution) {
        this.settings = new FrontierSettings(settings);
        this.settingsRevision = settingsRevision;
        this.requestId = requestId;
        this.resolution = resolution;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketFrontierSettings(FriendlyByteBuf buf) {
        this.settings = new FrontierSettings();
        if (buf.readableBytes() > 1) {
            this.settings.fromBytes(buf);
            settingsRevision = buf.readLong();
            requestId = buf.readLong();
            resolution = OperationResolution.VALUES[buf.readInt()];
        }
    }

    public void encode(FriendlyByteBuf buf) {
        settings.toBytes(buf);
        buf.writeLong(settingsRevision);
        buf.writeLong(requestId);
        buf.writeInt(resolution.ordinal());
    }

    public static void handle(PacketContext<PacketFrontierSettings> ctx) {
        PacketFrontierSettings message = ctx.message();
        if (Side.CLIENT.equals(ctx.side())) {
            ClientPacketDelivery.submit(() -> {
                if (Minecraft.getInstance().screen instanceof ModSettingsPage) {
                    ((ModSettingsPage) Minecraft.getInstance().screen).setFrontierSettings(message.settings,
                            message.settingsRevision, message.requestId, message.resolution);
                }
            });
        }
    }
}
