package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.screen.page.ModSettingsPage;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.server.settings.ServerSettingsOperationResult;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFrontierSettings {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_frontier_settings");

    private final FrontierSettings settings;

    public PacketFrontierSettings(FrontierSettings settings) {
        this.settings = settings;
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
            if (Minecraft.getInstance().screen instanceof ModSettingsPage) {
                ((ModSettingsPage) Minecraft.getInstance().screen).setFrontierSettings(message.settings);
            }
        }
    }
}
