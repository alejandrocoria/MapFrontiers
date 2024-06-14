package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketRequestFrontierSettings {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_request_frontier_settings");

    private int changeCounter;

    public PacketRequestFrontierSettings() {
        changeCounter = 0;
    }

    public PacketRequestFrontierSettings(int changeNonce) {
        this.changeCounter = changeNonce;
    }

    public static PacketRequestFrontierSettings decode(FriendlyByteBuf buf) {
        PacketRequestFrontierSettings packet = new PacketRequestFrontierSettings();

        try {
            if (buf.readableBytes() > 1) {
                packet.changeCounter = buf.readInt();
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketRequestFrontierSettings: %s", t));
        }

        return packet;
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
            if (player == null) {
                return;
            }
            FrontierSettings settings = FrontiersManager.instance.getSettings();

            if (settings.checkAction(FrontierSettings.Action.UpdateSettings, new SettingsUser(player),
                    MapFrontiers.isOPorHost(player), null) && settings.getChangeCounter() > message.changeCounter) {
                PacketHandler.sendTo(new PacketFrontierSettings(settings), player);
            } else {
                PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
            }
        }
    }
}
