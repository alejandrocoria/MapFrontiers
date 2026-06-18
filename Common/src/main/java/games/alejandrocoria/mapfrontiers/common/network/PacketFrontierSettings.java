package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.screen.page.ModSettingsPage;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.server.settings.ServerSettingsOperationResult;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFrontierSettings implements CustomPacketPayload {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_frontier_settings");
    public static final CustomPacketPayload.Type<PacketFrontierSettings> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFrontierSettings> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketFrontierSettings::encode, PacketFrontierSettings::new);

    private final FrontierSettings settings;

    public PacketFrontierSettings(FrontierSettings settings) {
        this.settings = settings;
    }

    @Override
    public CustomPacketPayload.Type<PacketFrontierSettings> type() {
        return TYPE;
    }

    public PacketFrontierSettings(FriendlyByteBuf buf) {
        this.settings = new FrontierSettings();
        if (buf.readableBytes() > 1) {
            this.settings.fromBytes(buf);
            this.settings.setChangeCounter(buf.readInt());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        settings.toBytes(buf);
        buf.writeInt(settings.getChangeCounter());
    }

    public static void handle(PacketContext<PacketFrontierSettings> ctx) {
        PacketFrontierSettings message = ctx.message();
        if (Side.SERVER.equals(ctx.side())) {
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerSettingsOperationResult result = MapFrontiers.getServerRuntime().getSettingsOperationService()
                    .updateSettings(player, message.settings);
            result.dispatchNetworkActions();
        } else if (Side.CLIENT.equals(ctx.side())) {
            if (Minecraft.getInstance().gui.screen() instanceof ModSettingsPage) {
                ((ModSettingsPage) Minecraft.getInstance().gui.screen()).setFrontierSettings(message.settings);
            }
        }
    }
}
