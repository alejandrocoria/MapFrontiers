package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.screen.ModSettings;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFrontierSettings {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_frontier_settings");

    private final FrontierSettings settings;

    public PacketFrontierSettings() {
        settings = new FrontierSettings();
    }

    public PacketFrontierSettings(FrontierSettings settings) {
        this.settings = settings;
    }

    public static PacketFrontierSettings decode(FriendlyByteBuf buf) {
        PacketFrontierSettings packet = new PacketFrontierSettings();
        packet.settings.fromBytes(buf);
        packet.settings.setChangeCounter(buf.readInt());
        return packet;
    }

    public void encode(FriendlyByteBuf buf) {
        settings.toBytes(buf);
        buf.writeInt(settings.getChangeCounter());
    }

    public static void handle(PacketContext<PacketFrontierSettings> ctx) {
        PacketFrontierSettings message = ctx.message();
        if (Side.SERVER.equals(ctx.side())) {
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            MinecraftServer server = player.server;
            if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.UpdateSettings,
                    new SettingsUser(player), MapFrontiers.isOPorHost(player), null)) {
                FrontiersManager.instance.setSettings(message.settings);

                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(p)), p);
                }
            } else {
                PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
            }
        } else if (Side.CLIENT.equals(ctx.side())) {
            if (Minecraft.getInstance().screen instanceof ModSettings) {
                ((ModSettings) Minecraft.getInstance().screen).setFrontierSettings(message.settings);
            }
        }
    }
}
